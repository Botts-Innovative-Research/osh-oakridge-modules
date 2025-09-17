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

package com.botts.impl.service.oscar;

import net.opengis.gml.v32.TimeIndeterminateValue;
import net.opengis.gml.v32.TimePosition;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.impl.sensor.AbstractSensorDriver;
import org.vast.swe.SWEConstants;

import java.util.Map;

public class OSCARSystem extends AbstractSensorDriver {

    public static String NAME = "OSCAR System";
    public static String DESCRIPTION = "System used for performing OSCAR operations";
    public static String UID = "urn:ornl:oscar:system:";

    protected OSCARSystem(String nodeId) {
        super(UID + nodeId, NAME);
        removeAllOutputs();
        removeAllControlInputs();
    }

    protected void updateSensorDescription() {
        if (lastUpdatedSensorDescription == Long.MIN_VALUE)
            lastUpdatedSensorDescription = System.currentTimeMillis();
        double newValidityTime = lastUpdatedSensorDescription / 1000.;

        // default IDs
        String gmlId = smlDescription.getId();
        if (gmlId == null || gmlId.isEmpty())
            smlDescription.setId(DEFAULT_XMLID_PREFIX + NAME.toUpperCase().replace(" ", "_"));
        if (!smlDescription.isSetIdentifier())
            smlDescription.setUniqueIdentifier(uniqueID);

        // name & description
        smlDescription.setDefinition(SWEConstants.DEF_SYSTEM);
        if (smlDescription.getName() == null)
            smlDescription.setName(NAME);
        if (smlDescription.getDescription() == null)
            smlDescription.setDescription(DESCRIPTION);

        // time validity
        if (smlDescription.getNumValidTimes() == 0)
        {
            GMLFactory fac = new GMLFactory();
            TimePosition begin = fac.newTimePosition(newValidityTime);
            TimePosition end = fac.newTimePosition();
            end.setIndeterminatePosition(TimeIndeterminateValue.NOW);
            smlDescription.addValidTimeAsTimePeriod(fac.newTimePeriod(begin, end));
        }

        // outputs
        if (smlDescription.getNumOutputs() == 0)
        {
            for (Map.Entry<String, ? extends IStreamingDataInterface> output: getOutputs().entrySet())
            {
                DataComponent outputDesc = output.getValue().getRecordDescription();
                if (outputDesc == null)
                    continue;
                outputDesc = outputDesc.copy();
                smlDescription.addOutput(output.getKey(), outputDesc);
            }
        }

        // control parameters
        if (smlDescription.getNumParameters() == 0)
        {
            for (Map.Entry<String, ? extends IStreamingControlInterface> param: getCommandInputs().entrySet())
            {
                DataComponent paramDesc = param.getValue().getCommandDescription();
                if (paramDesc == null)
                    continue;
                paramDesc = paramDesc.copy();
                paramDesc.setUpdatable(true);
                smlDescription.addParameter(param.getKey(), paramDesc);
            }
        }
    }

    @Override
    protected void addControlInput(IStreamingControlInterface controlInterface) {
        super.addControlInput(controlInterface);
    }

    @Override
    protected void addOutput(IStreamingDataInterface dataInterface, boolean isStatus) {
        super.addOutput(dataInterface, isStatus);
    }

    @Override
    public String getName() {
        return getShortID();
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
