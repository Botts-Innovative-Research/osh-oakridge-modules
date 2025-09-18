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

        // TODO: Add or update OSCAR system and client config system
        system = new OSCARSystem(config.nodeId);
        
        // TODO: Add or update report generation control interface

        // TODO: Add or update site info datastream

        createOutputs();
        createControls();

        system.updateSensorDescription();
        getParentHub().getSystemDriverRegistry().register(system);
    }

    public void createOutputs(){
        siteInfoOutput = new SiteInfoOutput(system);
        system.addOutput(siteInfoOutput, false);
        siteInfoOutput.init();

        clientConfigOutput = new ClientConfigOutput(system);
        system.addOutput(clientConfigOutput, false);
//        clientConfigOutput.init(); //move record strucutre to init to match all other modules
    }

    public void createControls(){
        reportControl = new RequestReportControl(system);
        system.addControlInput(reportControl);
//        reportControl.init();  move output record structure to init
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
