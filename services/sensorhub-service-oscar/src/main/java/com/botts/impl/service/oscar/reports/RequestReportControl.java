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
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.reports.helpers.EventReportType;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.types.*;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.*;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.vast.swe.SWEHelper;

import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static com.botts.impl.service.oscar.Constants.REPORT_BUCKET;

public class RequestReportControl extends AbstractControlInterface<OSCARSystem> implements IStreamingControlInterfaceWithResult {

    public static final String NAME = "requestReport";
    public static final String LABEL = "Request Report";
    public static final String DESCRIPTION = "Control to request operations, performance, and maintenance reports";

    public static final int RETRY_COUNT = 3;

    DataRecord commandStructure;
    DataComponent resultStructure;
    SWEHelper fac;

    OSCARServiceModule module;
    IBucketService bucketService;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

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
                        .optional(true)
                        .definition(SWEHelper.getPropertyUri("LaneUID"))
                        .description("Identifier of the lane(s) to request"))
                .addField("eventType", fac.createCategory()
                        .label("Event Type")
                        .optional(true)
                        .definition(SWEHelper.getPropertyUri("EventType"))
                        .description("Identifier of the event requested")
                        .addAllowedValues(EventReportType.class))
                .build();

        resultStructure = fac.createRecord().name("result")
                .addField("reportPath", fac.createText())
                .build();


        try {
            if (!bucketService.getBucketStore().bucketExists(REPORT_BUCKET))
                bucketService.getBucketStore().createBucket(REPORT_BUCKET);

        } catch (DataStoreException e) {
            module.getLogger().error("Bucket already exists.", e);
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

            String resourceURI = null;

            String filePath = null;
            IReportHandler reportHandler = null;
            try {
                filePath = buildPath(type, start, end, laneUIDs, eventType);
                reportHandler = getReportHandler(type, start, end, laneUIDs, eventType);
            } catch (DataStoreException e) {
                module.getLogger().error("Failed to build report " + type, e);
            }

            for (int retryAttempt = 0; retryAttempt < RETRY_COUNT; retryAttempt++) {
                try {
                    resourceURI = handleReportGeneration(reportHandler, filePath);

                    if (resourceURI != null) {
                        module.getLogger().info("Successfully generated report: " + resourceURI);
                        break;
                    }
                } catch (DataStoreException e) {
                    module.getLogger().error("Failed to build report " + type, e);
                }
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

    private IReportHandler getReportHandler(ReportCmdType type, Instant start, Instant end, String laneUIDs, EventReportType eventType) throws DataStoreException {
        if(type == ReportCmdType.LANE) {
            return out -> new LaneReport(out, start, end, laneUIDs, module);
        } else if (type == ReportCmdType.EVENT) {
            return out -> new EventReport(out, start, end, eventType, laneUIDs, module);
        } else if (type == ReportCmdType.ADJUDICATION) {
            return out -> new AdjudicationReport(out, start, end, laneUIDs, module);
        } else if (type == ReportCmdType.RDS_SITE) {
            return out -> new RDSReport(out, start, end, module);
        }
        return null;
    }

    private String handleReportGeneration(IReportHandler reportHandler, String filePath) throws DataStoreException {
        OutputStream out = checkBucketForOutputStream(filePath);

        if (out != null) {
            Report report = reportHandler.createReport(out);
            report.generate();
        }
        return bucketService.getBucketStore().getRelativeResourceURI(REPORT_BUCKET, filePath); // the command result is "reports/filePath.png"
    }

    // create file path here rather than repeating code at the top todo: possible add endtime to filepath
    private String buildPath(ReportCmdType type, Instant start, Instant end, String laneUIDs, EventReportType eventType) {
        StringBuilder sb = new StringBuilder();
        sb.append(module.getOSCARSystem().getNodeId()).append("_").append(type);

        if (type.equals(ReportCmdType.LANE) || type.equals(ReportCmdType.ADJUDICATION))
            sb.append("_").append(laneUIDs);
        else if (type.equals(ReportCmdType.EVENT))
            sb.append("_").append(eventType);

        sb.append("_").append(formatter.format(start)).append("_").append(".pdf");
        return sb.toString().toLowerCase().replace(":", "-");
    }

    // checks if a report file already exists in object store and only creates a new output if it doesnt exist
    private OutputStream checkBucketForOutputStream(String filePath) throws DataStoreException {
        if (!bucketService.getBucketStore().objectExists(REPORT_BUCKET, filePath)) {
            return bucketService.getBucketStore().putObject(REPORT_BUCKET, filePath, Collections.emptyMap());
        }
        return null;
    }
}