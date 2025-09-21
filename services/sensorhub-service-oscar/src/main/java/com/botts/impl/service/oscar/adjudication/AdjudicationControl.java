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

package com.botts.impl.service.oscar.adjudication;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.types.*;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class AdjudicationControl extends AbstractControlInterface<OSCARSystem> {

    public static final String NAME = "adjudicationControl";
    public static final String LABEL = "Adjudication Control";
    public static final String DESCRIPTION = "Control to adjudicate alarming occupancies";

    public static final String path = "files/adjudication/";

    DataComponent commandStructure;
    DataComponent resultStructure;
    SWEHelper fac;
    long startTime;

    OSCARServiceModule module;

    public AdjudicationControl(OSCARSystem parent, OSCARServiceModule module) {
        super(NAME, parent);

        fac = new SWEHelper();
        this.module = module;

        commandStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("observationId", fac.createText()
                        .definition(SWEHelper.getPropertyUri("ObservationID"))
                        .label("Observation ID")
                        .build())
                .addField("setAdjudicated", fac.createBoolean()
                        .label("Set Adjudicated")
                        .definition(SWEHelper.getPropertyUri("SetAdjudicated"))
                        .build())
                .build();

        resultStructure = fac.createRecord().name("result")
                .addField("isAdjudicated", fac.createBoolean()
                        .label("Adjudicated")
                        .definition(SWEHelper.getPropertyUri("IsAdjudicated"))
                        .build())
                .build();
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
            String observationId = paramData.getStringValue(0);
            boolean setAdjudicated = paramData.getBooleanValue(1);

            DataBlock resultData = resultStructure.createDataBlock();
            resultData.setStringValue(observationId);
            resultData.setBooleanValue(setAdjudicated);
            ICommandResult result = CommandResult.withData(resultData);

            ICommandStatus status = new CommandStatus.Builder()
                    .withCommand(command.getID())
                    .withStatusCode(ICommandStatus.CommandStatusCode.FAILED)
                    .withExecutionTime(TimeExtent.period(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(now)))
                    .withResult(result).build();
            return status;
        });
    }

}
