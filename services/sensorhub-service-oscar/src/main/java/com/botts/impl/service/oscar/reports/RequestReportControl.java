/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.reports;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.oscar.Constants;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.reports.helpers.EventReportType;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.types.*;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static com.botts.impl.service.oscar.Constants.REPORT_BUCKET;
import static com.botts.impl.service.oscar.Constants.SITE_MAP_BUCKET;

public class RequestReportControl extends AbstractControlInterface<OSCARSystem> implements IStreamingControlInterfaceWithResult {

    public static final String NAME = "requestReport";
    public static final String LABEL = "Request Report";
    public static final String DESCRIPTION = "Control to request operations, performance, and maintenance reports";

    DataRecord commandStructure;
    DataComponent resultStructure;
    SWEHelper fac;

    OSCARServiceModule module;
    IBucketService bucketService;
    Report report = null;

    public RequestReportControl(OSCARSystem parent, OSCARServiceModule module) {
        super(NAME, parent);

        fac = new SWEHelper();
        this.module = module;
        this.bucketService = module.getBucketService();

        commandStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("reportType", fac.createCategory()
                        .label("Report Type")
                        .definition(SWEHelper.getPropertyUri("ReportType"))
                        .description("Type of report to request")
                        .addAllowedValues(ReportCmdType.class))
                .addField("startDateTime", fac.createTime()
                        .definition(SWEHelper.getPropertyUri("StartDateTime"))
                        .withIso8601Format()
                        .description("Start datetime (ISO 8601)"))
                .addField("endDateTime", fac.createTime()
                        .definition(SWEHelper.getPropertyUri("EndDateTime"))
                        .withIso8601Format()
                        .description("End datetime (ISO 8601)"))
                .addField("laneUID", fac.createText()
                        .label("Lane Unique Identifier")
                        .definition(SWEHelper.getPropertyUri("LaneUID"))
                        .description("Identifier of the lane(s) to request"))
                .addField("eventType", fac.createCategory()
                        .label("Event Type")
                        .definition(SWEHelper.getPropertyUri("EventType"))
                        .description("Identifier of the event requested")
                        .addAllowedValues(EventReportType.class))
                .build();

        resultStructure = fac.createRecord().name("result")
                .addField("reportUrl", fac.createText())
                .build();


        try {
            if (!bucketService.getBucketStore().bucketExists(Constants.REPORT_BUCKET))
                bucketService.getBucketStore().createBucket(Constants.REPORT_BUCKET);

        } catch (DataStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        return CompletableFuture.supplyAsync(() -> {
            DataBlock paramData = command.getParams();

            ReportCmdType type = ReportCmdType.valueOf(paramData.getStringValue(0));
            Instant start = paramData.getTimeStamp(1);
            Instant end = paramData.getTimeStamp(2);
            String laneUIDs = paramData.getStringValue(3);
            EventReportType eventType = EventReportType.valueOf(paramData.getStringValue(4));

            String resourceURI = "";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
            String formattedStart = formatter.format(start);

            try{
                if (type.equals(ReportCmdType.LANE))
                    resourceURI = generateLaneReport(laneUIDs, formattedStart, start, end);
                else if (type.equals(ReportCmdType.EVENT))
                    resourceURI = generateEventReport(eventType, laneUIDs, formattedStart, start, end);
                else if (type.equals(ReportCmdType.ADJUDICATION))
                    resourceURI = generateAdjudicationReport(laneUIDs, formattedStart, start, end);
                else if (type.equals(ReportCmdType.RDS_SITE))
                    resourceURI = generateRDSReport(formattedStart, start, end);
            } catch (DataStoreException e) {
                throw new RuntimeException(e);
            }

            DataBlock resultData = resultStructure.createDataBlock();
            resultData.setStringValue(resourceURI);
            ICommandResult result = CommandResult.withData(resultData);

            return new CommandStatus.Builder()
                    .withCommand(command.getID())
                    .withStatusCode(resourceURI == null ? ICommandStatus.CommandStatusCode.FAILED : ICommandStatus.CommandStatusCode.ACCEPTED)
                    .withResult(result)
                    .build();
        });
    }

    @Override
    public DataComponent getResultDescription() {
        return resultStructure;
    }

    private String generateLaneReport(String laneUIDs, String formattedStart, Instant start, Instant end) throws DataStoreException {
        String filePath = ReportCmdType.LANE + "_" + laneUIDs + "_" + formattedStart + ".pdf";

        OutputStream out = checkBucketForOutputStream(filePath);
        if (out != null){
            report = new LaneReport(out, start, end, laneUIDs, module);
            report.generate();
        }

        return filePath;
    }

    private String generateEventReport(EventReportType eventType, String laneUIDs, String formattedStart, Instant start, Instant end) throws DataStoreException {
        String filePath = ReportCmdType.EVENT + "_" + eventType + "_" + laneUIDs + "_" + formattedStart + ".pdf";

        OutputStream out = checkBucketForOutputStream(filePath);

        if (out != null){
            report = new EventReport(out, start, end, eventType, laneUIDs, module);
            report.generate();
        }
        return filePath;
    }

    private String generateAdjudicationReport(String laneUIDs, String formattedStart, Instant start, Instant end) throws DataStoreException {
        String filePath = ReportCmdType.ADJUDICATION + "_" + laneUIDs + "_" + formattedStart + ".pdf";

        OutputStream out = checkBucketForOutputStream(filePath);

        if (out != null) {
            report = new AdjudicationReport(out, start, end, laneUIDs, module);
            report.generate();
        }
        return filePath;
    }

    private String generateRDSReport(String formattedStart, Instant start, Instant end) throws DataStoreException {
        String filePath = ReportCmdType.RDS_SITE + "_" + formattedStart + ".pdf";

        OutputStream out = checkBucketForOutputStream(filePath);

        // only generate a new report if file doesnt exist in bucket store :D
        if (out != null) {
            report = new RDSReport(out, start, end, module);
            report.generate();
        }
        return filePath;
    }

    // checks if a report file already exists in object store and only creates a new output if it doesnt exist
    private OutputStream checkBucketForOutputStream(String filePath) throws DataStoreException {
        if (!bucketService.getBucketStore().objectExists(REPORT_BUCKET, filePath)) {
            return bucketService.getBucketStore().putObject(REPORT_BUCKET, filePath, Collections.emptyMap());
        }
        return null;
    }

}