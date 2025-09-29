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

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.reports.helpers.EventReportType;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.types.*;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;
import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class RequestReportControl extends AbstractControlInterface<OSCARSystem> implements IStreamingControlInterfaceWithResult {

    public static final String NAME = "requestReport";
    public static final String LABEL = "Request Report";
    public static final String DESCRIPTION = "Control to request operations, performance, and maintenance reports";

    public static final String bucketPath = "/reports/";

    DataComponent commandStructure;
    DataComponent resultStructure;
    SWEHelper fac;

    OSCARServiceModule module;

    public RequestReportControl(OSCARSystem parent, OSCARServiceModule module) {
        super(NAME, parent);

        fac = new SWEHelper();
        this.module = module;

        commandStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("startDateTime", fac.createTime()
                        .definition(SWEHelper.getPropertyUri("StartDateTime"))
                        .withIso8601Format()
                        .description("Start datetime (ISO 8601)"))
                .addField("endDateTime", fac.createTime()
                        .definition(SWEHelper.getPropertyUri("EndDateTime"))
                        .withIso8601Format()
                        .description("End datetime (ISO 8601)"))
                .addField("reportType", fac.createCategory()
                        .label("Report Type")
                        .definition(SWEHelper.getPropertyUri("ReportType"))
                        .description("Type of report to request")
                        .addAllowedValues(ReportCmdType.class))
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
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {

        return CompletableFuture.supplyAsync(() -> {

            DataBlock paramData = command.getParams();

            Instant start = paramData.getTimeStamp(0);
            Instant end = paramData.getTimeStamp(1);
            ReportCmdType type = ReportCmdType.valueOf(paramData.getStringValue(2));
            String laneUIDs = paramData.getStringValue(3);
            EventReportType eventType = EventReportType.valueOf(paramData.getStringValue(4));

            Report report = null;
            File file;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
            String formattedStart = formatter.format(start);

            if(type.equals(ReportCmdType.LANE)) {
                for (var lane: laneUIDs.split(",")) {
                    String filePath = bucketPath + type + "_" + lane + "_" + formattedStart + ".pdf";
                    file = new File(filePath);

                    if (!file.exists())
                        report = new LaneReport(filePath, start, end, lane, module);
                }
            } else if(type.equals(ReportCmdType.EVENT)) {
                for (var lane: laneUIDs.split(",")) {
                    String filePath = bucketPath + type + "_" + eventType +  "_" +  lane + "_" + formattedStart + ".pdf";
                    file = new File(filePath);

                    if (!file.exists())
                        report = new EventReport(filePath, start, end, eventType, lane, module);
                }
            } else if (type.equals(ReportCmdType.ADJUDICATION)) {
                for (var lane: laneUIDs.split(",")) {
                    String filePath = bucketPath + type + "_" + lane + "_" + formattedStart + ".pdf";
                    file = new File(filePath);

                    if (!file.exists())
                        report = new AdjudicationReport(filePath, start, end, lane, module);
                }
            } else if (type.equals(ReportCmdType.RDS_SITE)) {
                String filePath = bucketPath + type + "_" +  formattedStart + ".pdf";
                file = new File(filePath);

                if (!file.exists())
                    report = new RDSReport(filePath, start, end, module);
            }


            assert report != null;
            String url = report.generate();

            DataBlock resultData = resultStructure.createDataBlock();
            resultData.setStringValue(url);
            ICommandResult result = CommandResult.withData(resultData);


            return new CommandStatus.Builder()
                    .withCommand(command.getID())
                    .withStatusCode(url == null ? ICommandStatus.CommandStatusCode.FAILED : ICommandStatus.CommandStatusCode.ACCEPTED)
                    .withResult(result)
                    .build();
        });
    }

    @Override
    public DataComponent getResultDescription() {
        return resultStructure;
    }
}
