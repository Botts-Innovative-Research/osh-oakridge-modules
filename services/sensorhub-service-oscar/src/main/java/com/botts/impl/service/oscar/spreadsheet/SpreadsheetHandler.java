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

package com.botts.impl.service.oscar.spreadsheet;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.oscar.IFileHandler;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.LaneConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.module.ModuleRegistry;
import org.slf4j.Logger;
import org.vast.util.Asserts;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

public class SpreadsheetHandler implements IFileHandler {

    ModuleRegistry reg;
    SpreadsheetParser parser;
    IBucketStore store;
    Logger logger;

    public static String SPREADSHEET_BUCKET = "spreadsheets";
    public static String CONFIG_KEY = "config.csv";

    public SpreadsheetHandler(ModuleRegistry reg, IBucketStore bucketStore, Logger logger) {
        this.reg = reg;
        this.store = Asserts.checkNotNull(bucketStore);
        this.logger = logger;
        if (!store.bucketExists(SPREADSHEET_BUCKET)) {
            try {
                store.createBucket(SPREADSHEET_BUCKET);
            } catch (DataStoreException e) {
                logger.error("Unable to create bucket for spreadsheet config", e);
            }
        }
        this.parser = new SpreadsheetParser();
    }

    @Override
    public OutputStream handleUpload(String filename) throws DataStoreException {
        return store.putObject(SPREADSHEET_BUCKET, filename, Collections.emptyMap());
    }

    @Override
    public boolean handleFile(String filename) {
        // Check file got saved successfully
        if (!store.objectExists(SPREADSHEET_BUCKET, filename))
            return false;
        try {
            // Get object stream and load it to the module registry
            var stream = store.getObject(SPREADSHEET_BUCKET, filename);
            handleCSV(new String(stream.readAllBytes()));
        } catch (IOException | SensorHubException e) {
            logger.error("Could not load config file " + filename);
            return false;
        }
        return true;
    }

    @Override
    public boolean isValidFileType(String fileName, String mimeType) {
        return fileName.endsWith(".csv") || mimeType.contains("csv");
    }

    public InputStream getDownloadStream() {
        // Check that we have lanes
        var lanes = reg.getLoadedModules(LaneSystem.class);
        if (lanes == null || lanes.isEmpty())
            return null;
        var laneConfigs = lanes.stream().map(LaneSystem::getConfiguration).toList();
        if (laneConfigs.isEmpty())
            return null;
        // Serialize current config to spreadsheet
        var serialized = parser.serialize(laneConfigs);
        try {
            // Save spreadsheet just in case file gets lost
            store.putObject(SPREADSHEET_BUCKET, CONFIG_KEY,
                    new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8)),
                    Collections.emptyMap());
            return store.getObject(SPREADSHEET_BUCKET, CONFIG_KEY);
        } catch (DataStoreException e) {
            logger.error("Unable to save and serve spreadsheet");
            return null;
        }
    }

    private void handleCSV(String csvData) throws IOException, SensorHubException {
        Collection<LaneConfig> lanes = parser.deserialize(csvData);
        loadModules(lanes);
    }

    public void loadModules(Collection<LaneConfig> laneConfigs) throws SensorHubException {
        for (var config : laneConfigs)
            reg.loadModule(config);
    }

}
