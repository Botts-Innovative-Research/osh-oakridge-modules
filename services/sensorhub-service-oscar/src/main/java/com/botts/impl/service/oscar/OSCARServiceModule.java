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
import com.botts.impl.service.oscar.adjudication.AdjudicationControl;
import com.botts.impl.service.oscar.clientconfig.ClientConfigOutput;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.siteinfo.SiteInfoOutput;
import com.botts.impl.service.oscar.siteinfo.SitemapDiagramHandler;
import com.botts.impl.service.oscar.spreadsheet.SpreadsheetHandler;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.impl.module.AbstractModule;

import java.io.FileNotFoundException;

public class OSCARServiceModule extends AbstractModule<OSCARServiceConfig> {
    SiteInfoOutput siteInfoOutput;
    RequestReportControl reportControl;
    AdjudicationControl adjudicationControl;

    ClientConfigOutput clientConfigOutput;
    SpreadsheetHandler spreadsheetHandler;
    SitemapDiagramHandler sitemapDiagramHandler;
    OSCARSystem system;

    IBucketService bucketService;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        system = new OSCARSystem(config.nodeId);

        bucketService = getParentHub().getModuleRegistry().getModuleByType(IBucketService.class);


        spreadsheetHandler = new SpreadsheetHandler(getParentHub());
        if (config.spreadsheetConfigPath != null && !config.spreadsheetConfigPath.isEmpty())
            spreadsheetHandler.handleFile(config.spreadsheetConfigPath);

        // TODO: Add or update site info datastream

        sitemapDiagramHandler = new SitemapDiagramHandler(getBucketService());

        if(config.siteDiagramConfig.siteDiagramPath != null && !config.siteDiagramConfig.siteDiagramPath.isEmpty()){
            try {
                sitemapDiagramHandler.handleFile(config.siteDiagramConfig.siteDiagramPath);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }


        createOutputs();
        createControls();

        system.updateSensorDescription();
        getParentHub().getSystemDriverRegistry().register(system);

        var module = getParentHub().getModuleRegistry().getModuleById(config.databaseID);
        getParentHub().getSystemDriverRegistry().registerDatabase(system.getUniqueIdentifier(), (IObsSystemDatabase) module);

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

        adjudicationControl = new AdjudicationControl(system, this);
        system.addControlInput(adjudicationControl);
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();


        // TODO: Publish latest site info observation
        if (config.siteDiagramConfig != null
                && config.siteDiagramConfig.siteDiagramPath != null
                && !config.siteDiagramConfig.siteDiagramPath.isEmpty()
                && config.siteDiagramConfig.siteLowerLeftBound != null
                && config.siteDiagramConfig.siteUpperRightBound != null) {
            siteInfoOutput.setData(config.siteDiagramConfig.siteDiagramPath, config.siteDiagramConfig.siteLowerLeftBound, config.siteDiagramConfig.siteUpperRightBound);
        }
    }

    public SitemapDiagramHandler getSitemapDiagramHandler() {
        return sitemapDiagramHandler;
    }

    public SpreadsheetHandler getSpreadsheetHandler() {
        return spreadsheetHandler;
    }

    public IBucketService getBucketService() {
        return bucketService;
    }
    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
    }

}
