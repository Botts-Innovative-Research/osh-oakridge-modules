/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rapiscan.control;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.utils.rad.RADHelper;

/**
 * Control input used to manually publish the Rapiscan tamper state.
 */
public class ManualTamperControl extends AbstractSensorControl<RapiscanSensor> {

    public static final String NAME = "manualTamper";
    public static final String LABEL = "Manual Tamper";

    private final DataComponent commandData;

    public ManualTamperControl(RapiscanSensor parentSensor) {
        super(NAME, parentSensor);

        var radHelper = new RADHelper();
        commandData = radHelper.createBoolean()
                .name(NAME)
                .label(LABEL)
                .description("Manually publish the Rapiscan tamper state")
                .definition(RADHelper.DEF_TAMPER)
                .build();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandData;
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {
        parentSensor.getTamperOutput().onNewMessage(command.getBooleanValue());
        return true;
    }
}
