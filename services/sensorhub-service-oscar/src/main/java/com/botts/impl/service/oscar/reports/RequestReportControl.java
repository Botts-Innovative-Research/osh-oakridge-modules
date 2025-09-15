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

import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.reports.helpers.ReportType;
import com.botts.impl.service.oscar.reports.types.*;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class RequestReportControl extends AbstractControlInterface<OSCARSystem> {

    public static final String NAME = "requestReport";
    public static final String LABEL = "Request Report";
    public static final String DESCRIPTION = "Control to request operations, performance, and maintenance reports";

    public static final String path = "files/reports/";

    DataComponent commandStructure;
    DataComponent resultStructure;
    SWEHelper fac;
    long startTime;

    protected RequestReportControl(OSCARSystem parent) {
        super(NAME, parent);
        fac = new SWEHelper();

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
                        .addAllowedValues(ReportType.class))
                .addField("laneId", fac.createText()
                        .label("Lane ID")
                        .definition(SWEHelper.getPropertyUri("LaneID"))
                        .description("Identifier of the lane to request"))
                .addField("eventID", fac.createText()
                        .label("Event ID")
                        .definition(SWEHelper.getPropertyUri("EventID"))
                        .description("Identifier of the event requested"))
                .build();

        // TODO: Either have 2 urls for local and remote URL, or just handle this in client
        // e.g. localhost:8282/reports/report123.pdf vs. public.ip:8282/reports/report123.pdf
        resultStructure = fac.createText().name("reportUrl").build();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {

        return CompletableFuture.supplyAsync(() -> {

            long now = System.currentTimeMillis();

            if (startTime == 0)
                this.startTime = now;

            DataBlock paramData = command.getParams();
            Instant start = paramData.getTimeStamp(0);
            Instant end = paramData.getTimeStamp(1);
            ReportType type = ReportType.valueOf(paramData.getStringValue(2));
            String laneId = paramData.getStringValue(3);
            String eventId = paramData.getStringValue(4);

            Report report = null;

            File file;

            switch (type) {
                case LANE -> {
                    file = new File(path + type + "_" + laneId + "_" + start + "_" + end + ".pdf");

                    if (!file.exists())
                        report = new LaneReport(start, end, laneId);
                }
                case RDS_SITE -> {
                    file = new File(path + type + "_" + start + "_" + end + ".pdf");

                    if (!file.exists())
                        report = new RDSReport(start, end);
                }
                case EVENT -> {
                    file = new File(path + type + "_" + laneId + "_" + eventId + "_" + start + "_" + end + ".pdf");

                    if (!file.exists())
                        report = new EventReportTodo(start, end, eventId, laneId);
                }
                case ADJUDICATION -> {
                    file = new File(path + type + "_" + laneId + "_" + eventId + "_" + start + "_" + end + ".pdf");
                    if (!file.exists())
                        report = new AdjudicationReport(start, end, eventId, laneId);
                }
                default -> report = null;
            }

            if (report == null) System.out.println("Report not found");


            String url = report.generate();

            ICommandStatus status = null;

            // if report is invalid then send FAILED command status

            // TODO: Build command result
            DataBlock resultData = resultStructure.createDataBlock();
            resultData.setStringValue(url);
            ICommandResult result = CommandResult.withData(resultData);

            if (url == null) {
                status = new CommandStatus.Builder()
                        .withCommand(command.getID())
                        .withStatusCode(ICommandStatus.CommandStatusCode.FAILED)
                        .withExecutionTime(TimeExtent.period(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(now)))
                        .withResult(result).build();
            } else {
                status = new CommandStatus.Builder()
                        .withCommand(command.getID())
                        .withStatusCode(ICommandStatus.CommandStatusCode.ACCEPTED)
                        .withExecutionTime(TimeExtent.period(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(now)))
                        .withResult(result)
                        .build();
            }

            return status;
        });
    }

}
