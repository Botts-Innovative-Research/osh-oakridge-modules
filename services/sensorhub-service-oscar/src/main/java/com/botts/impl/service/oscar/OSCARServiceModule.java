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

import com.botts.impl.service.oscar.clientconfig.ClientConfigOutput;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.siteinfo.SiteInfoOutput;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;

public class OSCARServiceModule extends AbstractModule<OSCARServiceConfig> {


    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // TODO: Add or update OSCAR system and client config system
        OSCARSystem system = new OSCARSystem(config.nodeId);

        RequestReportControl reportControl = new RequestReportControl(system);
        system.addControlInput(reportControl);

        SiteInfoOutput siteInfoOutput = new SiteInfoOutput(system);
        system.addOutput(siteInfoOutput, false);

        ClientConfigOutput clientConfigOutput = new ClientConfigOutput(system);
        system.addOutput(clientConfigOutput, false);
        // TODO: Add or update report generation control interface

        // TODO: Add or update site info datastream
        system.updateSensorDescription();
        getParentHub().getSystemDriverRegistry().register(system);
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        // TODO: Publish latest site info observation

    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
    }

}
