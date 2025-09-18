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

import java.util.List;

public class OSCARServiceModule extends AbstractModule<OSCARServiceConfig> {
    SiteInfoOutput siteInfoOutput;
    RequestReportControl reportControl;

    ClientConfigOutput clientConfigOutput;

    OSCARSystem system;
    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        system = new OSCARSystem(config.nodeId);
        
        createOutputs();
        createControls();

        system.updateSensorDescription();
        getParentHub().getSystemDriverRegistry().register(system);
    }

    public void createOutputs() {
        siteInfoOutput = new SiteInfoOutput(system);
        system.addOutput(siteInfoOutput, false);
        siteInfoOutput.init();

        clientConfigOutput = new ClientConfigOutput(system);
        system.addOutput(clientConfigOutput, false);
        clientConfigOutput.init();
    }
    public void createControls(){
        reportControl = new RequestReportControl(system);
        system.addControlInput(reportControl);
        reportControl.init();
    }
    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        // TODO: Publish latest site info observation
        siteInfoOutput.setData(config.siteDiagramConfig.siteDiagramPath, config.siteDiagramConfig.siteLowerLeftBound, config.siteDiagramConfig.siteUpperRightBound);
    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
    }

}
