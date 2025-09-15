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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;

public class OSCARServiceModule extends AbstractModule<OSCARServiceConfig> {


    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // TODO: Add or update OSCAR system and client config system
        OSCARSystem system = new OSCARSystem(config.nodeId);

        // TODO: Check that multiple restarts / reloads doesn't overwrite OSCAR system
        getParentHub().getSystemDriverRegistry().register(system);

        // TODO: Add or update report generation control interface

        // TODO: Add or update site info datastream
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        // TODO: Publish latest site info observation

//        getParentHub().getSystemDriverRegistry().getDatabase("urn:osh:system:lane:1").getObservationStore();
//        getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().get("sensorhub").getParameters();

    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
    }

}
