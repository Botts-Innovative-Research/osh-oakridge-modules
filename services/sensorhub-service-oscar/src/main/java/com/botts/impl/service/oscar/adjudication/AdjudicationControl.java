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
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class AdjudicationControl extends AbstractSensorControl<OSCARSystem> {

    public static final String NAME = "adjudicationControl";
    public static final String LABEL = "Adjudication Control";
    public static final String DESCRIPTION = "Control to adjudicate alarming occupancies";

    public static final String path = "files/adjudication/";

    DataRecord commandData;

    SWEHelper fac;
    IdEncoder obsEncoder;
    OSCARServiceModule module;

    public AdjudicationControl(OSCARSystem parent, OSCARServiceModule module) {
        super(NAME, parent);

        fac = new SWEHelper();
        this.module = module;

        commandData = fac.createRecord()
                .name(getName())
                .label(LABEL)
                .addField("observationId", fac.createText()
                        .label("Observation ID")
                        .build())
                .addField("setAdjudicated", fac.createBoolean()
                        .label("Set Adjudicated")
                        .build())
                .build();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandData;
    }

    @Override
    protected boolean execCommand(DataBlock command) {

        DataRecord commandMsg = commandData.copy();
        commandMsg.setData(command);

        String observationId = commandMsg.getData().getStringValue(0);
        boolean setAdjudicated = commandMsg.getData().getBooleanValue(1);

        if(observationId == null || observationId.equals("null"))
            return false;

        BigId internalObsId = obsEncoder.decodeID(observationId);

        var prevObs = module.getParentHub().getDatabaseRegistry()
                .getFederatedDatabase()
                .getObservationStore().get(internalObsId);

        prevObs.getResult().setBooleanValue(9, setAdjudicated);

        String systemUID = getParentProducer().getParentSystemUID() != null ?
                getParentProducer().getParentSystemUID() :
                getParentProducer().getUniqueIdentifier();

        var obsDb = module.getParentHub().getSystemDriverRegistry().getDatabase(systemUID);

        obsDb.getObservationStore().put(internalObsId, prevObs);

        return prevObs.getResult().getBooleanValue(9) == setAdjudicated;

    }



}
