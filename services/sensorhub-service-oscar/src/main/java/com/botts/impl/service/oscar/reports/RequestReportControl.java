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
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class RequestReportControl extends AbstractControlInterface<OSCARSystem> implements IStreamingControlInterfaceWithResult {

    public static final String NAME = "requestReport";
    public static final String LABEL = "Request Report";
    public static final String DESCRIPTION = "Control to request operations, performance, and maintenance reports";

    DataComponent commandStructure;
    DataComponent resultStructure;
    SWEHelper fac;

    enum ReportType {
        RDS_SITE,
        LANE,
        OPERATIONS,
        EVENT,
        SECONDARY_INSPECTIONS
    }

    public RequestReportControl(OSCARSystem parent) {
        super(NAME, parent);
        fac = new SWEHelper();

        // TODO: Add additional parameters for lane UID, event ID, etc.
        // TODO: Add defs
        commandStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("startDateTime", fac.createTime()
                        .withIso8601Format()
                        .description("Start datetime (ISO 8601)"))
                .addField("endDateTime", fac.createTime()
                        .withIso8601Format()
                        .description("End datetime (ISO 8601)"))
                .addField("reportType", fac.createCategory()
                        .description("Report type to request")
                        .addAllowedValues(ReportType.class))
                .build();

        // TODO: Either have 2 urls for local and remote URL, or just handle this in client
        // e.g. localhost:8282/reports/report123.pdf vs. public.ip:8282/reports/report123.pdf
        resultStructure = fac.createRecord().name("result")
                .addField("reportUrl", fac.createText())
                .build();
    }

    @Override
    public DataComponent getResultDescription() {
        return resultStructure;
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        return CompletableFuture.supplyAsync(() -> {
//            DataBlock paramData = command.getParams();
//            Instant start = paramData.getTimeStamp(0);
//            Instant end = paramData.getTimeStamp(1);
//            ReportType type = ReportType.valueOf(paramData.getStringValue(2));

            // TODO: Check if report of this type and during this time frame already exists in the filesystem
            // Report report = getReport(start, end, type);
            // if report == null then generate new report

            // if report is invalid then send FAILED command status

            // TODO: Build command result
            DataBlock resultData = resultStructure.createDataBlock();
            resultData.setStringValue("URL of report PDF in file system");
            ICommandResult result = CommandResult.withData(resultData);

            // TODO: Build status
            ICommandStatus status = new CommandStatus.Builder()
                    .withCommand(command.getID())
                    .withStatusCode(ICommandStatus.CommandStatusCode.ACCEPTED)
                    .withResult(result)
                    .build();

            return status;
        });
    }

}
