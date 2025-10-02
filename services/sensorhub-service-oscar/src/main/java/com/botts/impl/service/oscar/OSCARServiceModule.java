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

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.oscar.clientconfig.ClientConfigOutput;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.siteinfo.SiteInfoOutput;
import com.botts.impl.service.oscar.siteinfo.SitemapDiagramHandler;
import com.botts.impl.service.oscar.spreadsheet.SpreadsheetHandler;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.AbstractModule;

import java.util.concurrent.ExecutionException;

public class OSCARServiceModule extends AbstractModule<OSCARServiceConfig> {
    SiteInfoOutput siteInfoOutput;
    RequestReportControl reportControl;
    ClientConfigOutput clientConfigOutput;
    SitemapDiagramHandler sitemapDiagramHandler;
    IBucketService bucketService;

    SpreadsheetHandler spreadsheetHandler;
    OSCARSystem system;
    IBucketStore bucketStore;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // Block here for bucket service
        try {
            this.bucketService = getParentHub().getModuleRegistry()
                    .waitForModuleType(IBucketService.class, ModuleEvent.ModuleState.STARTED)
                    .get();
            this.bucketStore = bucketService.getBucketStore();
        } catch (InterruptedException | ExecutionException e) {
            reportError("Could not find this OSH node's Bucket Service", new IllegalStateException(e));
        }

        spreadsheetHandler = new SpreadsheetHandler(getParentHub().getModuleRegistry(), bucketStore, getLogger());
        if (config.spreadsheetConfigPath != null && !config.spreadsheetConfigPath.isEmpty())
            spreadsheetHandler.handleFile(config.spreadsheetConfigPath);

        system = new OSCARSystem(config.nodeId);
        bucketService = getParentHub().getModuleRegistry().getModuleByType(IBucketService.class);

        createOutputs();
        createControls();

        sitemapDiagramHandler = new SitemapDiagramHandler(getBucketService(), siteInfoOutput, this);

        system.updateSensorDescription();
    }

    public void createOutputs(){

        siteInfoOutput = new SiteInfoOutput(system);
        system.addOutput(siteInfoOutput, false);

        clientConfigOutput = new ClientConfigOutput(system);
        system.addOutput(clientConfigOutput, false);
    }

    public void createControls(){
        reportControl = new RequestReportControl(system, this);
        system.addControlInput(reportControl);
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        getParentHub().getSystemDriverRegistry().register(system);

        var module = getParentHub().getModuleRegistry().getModuleById(config.databaseID);
        if (getParentHub().getSystemDriverRegistry().getDatabase(system.getUniqueIdentifier()) == null)
            getParentHub().getSystemDriverRegistry().registerDatabase(system.getUniqueIdentifier(), (IObsSystemDatabase) module);

    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
    }

    public SpreadsheetHandler getSpreadsheetHandler() {
        return spreadsheetHandler;
    }

    public SitemapDiagramHandler getSitemapDiagramHandler() {
        return sitemapDiagramHandler;
    }


    public OSCARSystem getOSCARSystem() {
        return system;
    }
    public IBucketService getBucketService() {
        return bucketService;
    }

}
