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

import org.sensorhub.impl.sensor.AbstractSensorDriver;

public class OSCARSystem extends AbstractSensorDriver {

    public static String NAME = "OSCAR System";
    public static String DESCRIPTION = "System used for performing OSCAR operations";
    public static String UID = "urn:ornl:oscar:system:";

    protected OSCARSystem(String nodeId) {
        super(UID + nodeId, NAME);
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
