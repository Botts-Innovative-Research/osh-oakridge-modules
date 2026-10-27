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
import com.botts.impl.service.oscar.OSCARServiceConfig;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.LaneConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.ModuleRegistry;
import org.slf4j.Logger;
import org.vast.util.Asserts;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class SpreadsheetHandler implements IFileHandler {

    ModuleRegistry reg;
    SpreadsheetParser parser;
    IBucketStore store;
    Logger logger;
    OSCARServiceConfig oscarConfig;

    public static String SPREADSHEET_BUCKET = "spreadsheets";
    public static String CONFIG_KEY = "config.csv";

    public SpreadsheetHandler(ModuleRegistry reg, IBucketStore bucketStore, Logger logger) {
        this(reg, bucketStore, logger, null);
    }

    public SpreadsheetHandler(ModuleRegistry reg, IBucketStore bucketStore, Logger logger, OSCARServiceConfig oscarConfig) {
        this.reg = reg;
        this.store = Asserts.checkNotNull(bucketStore);
        this.logger = logger;
        this.oscarConfig = oscarConfig;
        if (!store.bucketExists(SPREADSHEET_BUCKET)) {
            try {
                store.createBucket(SPREADSHEET_BUCKET);
            } catch (DataStoreException e) {
                logger.error("Unable to create bucket for spreadsheet config", e);
            }
        }
        this.parser = new SpreadsheetParser();
    }

    public SpreadsheetParser getParser() {
        return parser;
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
        try {
            syncRetentionPolicyFromModules();
            // Serialize current config to spreadsheet
            var serialized = parser.serialize(laneConfigs);

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
        applyRetentionPolicyToModules();
        loadModules(lanes);
    }

    private OSCARServiceConfig resolveOscarConfig() {
        if (oscarConfig != null)
            return oscarConfig;
        if (reg == null)
            return null;
        var modules = reg.getLoadedModules(OSCARServiceModule.class);
        if (modules != null && !modules.isEmpty())
            return modules.iterator().next().getConfiguration();
        return null;
    }

    void syncRetentionPolicyFromModules() {
        SpreadsheetParser.RetentionPolicyConfig retention = parser.getRetentionPolicyConfig();
        retention.readFromOscarConfig(resolveOscarConfig());
        if (reg == null)
            return;
        for (IModule<?> module : reg.getLoadedModules()) {
            Object cfg = module.getConfiguration();
            if (cfg == null)
                continue;
            String className = cfg.getClass().getName();
            if ("com.botts.impl.service.dailyfile.DailyFileJournalServiceConfig".equals(className)) {
                readDailyFileConfig(cfg, retention);
            } else if ("org.sensorhub.impl.database.system.SystemDriverDatabaseConfig".equals(className)) {
                Object dbConfig = getFieldValue(cfg, "dbConfig");
                if (dbConfig != null)
                    readArchiveConfig(getFieldValue(dbConfig, "archive"), retention);
            } else if ("org.sensorhub.impl.datastore.postgis.database.OscarTieredObsSystemDatabaseConfig".equals(className)) {
                readArchiveConfig(getFieldValue(cfg, "archive"), retention);
            }
        }
    }

    void applyRetentionPolicyToModules() {
        SpreadsheetParser.RetentionPolicyConfig retention = parser.getRetentionPolicyConfig();
        retention.applyToOscarConfig(resolveOscarConfig());
        if (reg == null)
            return;
        for (IModule<?> module : reg.getLoadedModules()) {
            Object cfg = module.getConfiguration();
            if (cfg == null)
                continue;
            String className = cfg.getClass().getName();
            if ("com.botts.impl.service.dailyfile.DailyFileJournalServiceConfig".equals(className)) {
                writeDailyFileConfig(cfg, retention);
            } else if ("org.sensorhub.impl.database.system.SystemDriverDatabaseConfig".equals(className)) {
                Object dbConfig = getFieldValue(cfg, "dbConfig");
                if (dbConfig != null)
                    writeArchiveConfig(getFieldValue(dbConfig, "archive"), retention);
            } else if ("org.sensorhub.impl.datastore.postgis.database.OscarTieredObsSystemDatabaseConfig".equals(className)) {
                writeArchiveConfig(getFieldValue(cfg, "archive"), retention);
            }
        }
    }

    private static void readDailyFileConfig(Object cfg, SpreadsheetParser.RetentionPolicyConfig retention) {
        Object mode = getFieldValue(cfg, "dailyFileObservationMode");
        if (mode != null)
            retention.dailyFileObservationMode = mode.toString();
        Object days = getFieldValue(cfg, "sealedJournalRetentionDays");
        if (days instanceof Number number)
            retention.sealedJournalRetentionDays = number.intValue();
        Object backup = getFieldValue(cfg, "requireBackupBeforeSealedJournalDeletion");
        if (backup instanceof Boolean bool)
            retention.requireBackupBeforeSealedJournalDeletion = bool;
    }

    private static void writeDailyFileConfig(Object cfg, SpreadsheetParser.RetentionPolicyConfig retention) {
        setEnumFieldValue(cfg, "dailyFileObservationMode", retention.dailyFileObservationMode);
        setFieldValue(cfg, "sealedJournalRetentionDays", retention.sealedJournalRetentionDays);
        setFieldValue(cfg, "requireBackupBeforeSealedJournalDeletion", retention.requireBackupBeforeSealedJournalDeletion);
    }

    private static void readArchiveConfig(Object archive, SpreadsheetParser.RetentionPolicyConfig retention) {
        if (archive == null)
            return;
        if (getFieldValue(archive, "enabled") instanceof Boolean b)
            retention.archiveEnabled = b;
        Object mode = getFieldValue(archive, "storageMode");
        if (mode != null)
            retention.archiveStorageMode = mode.toString();
        if (getFieldValue(archive, "hotRetentionDays") instanceof Number n)
            retention.archiveHotRetentionDays = n.intValue();
        if (getFieldValue(archive, "parquetPrimaryArchiveDelayMinutes") instanceof Number n)
            retention.archiveParquetPrimaryDelayMinutes = n.intValue();
        if (getFieldValue(archive, "immutableStageWindowMinutes") instanceof Number n)
            retention.archiveImmutableStageWindowMinutes = n.intValue();
        if (getFieldValue(archive, "maximumFuturePhenomenonSkewMinutes") instanceof Number n)
            retention.archiveMaximumFutureSkewMinutes = n.intValue();
        if (getFieldValue(archive, "parquetPrimaryIntervalMinutes") instanceof Number n)
            retention.archiveParquetPrimaryIntervalMinutes = n.intValue();
        if (getFieldValue(archive, "maximumStagePartitionsPerCycle") instanceof Number n)
            retention.archiveMaximumStagePartitionsPerCycle = n.intValue();
        if (getFieldValue(archive, "intervalMinutes") instanceof Number n)
            retention.archiveIntervalMinutes = n.intValue();
        if (getFieldValue(archive, "maximumGenerationsPerCycle") instanceof Number n)
            retention.archiveMaximumGenerationsPerCycle = n.intValue();
        if (getFieldValue(archive, "maximumVerificationsPerCycle") instanceof Number n)
            retention.archiveMaximumVerificationsPerCycle = n.intValue();
        if (getFieldValue(archive, "maximumDeletionsPerCycle") instanceof Number n)
            retention.archiveMaximumDeletionsPerCycle = n.intValue();
        if (getFieldValue(archive, "maximumCycleMinutes") instanceof Number n)
            retention.archiveMaximumCycleMinutes = n.intValue();
        if (getFieldValue(archive, "rollbackGraceDays") instanceof Number n)
            retention.archiveRollbackGraceDays = n.intValue();
        if (getFieldValue(archive, "deleteHotAfterArchive") instanceof Boolean b)
            retention.archiveDeleteHotAfterArchive = b;
    }

    private static void writeArchiveConfig(Object archive, SpreadsheetParser.RetentionPolicyConfig retention) {
        if (archive == null)
            return;
        setFieldValue(archive, "enabled", retention.archiveEnabled);
        setEnumFieldValue(archive, "storageMode", retention.archiveStorageMode);
        setFieldValue(archive, "hotRetentionDays", retention.archiveHotRetentionDays);
        setFieldValue(archive, "parquetPrimaryArchiveDelayMinutes", retention.archiveParquetPrimaryDelayMinutes);
        setFieldValue(archive, "immutableStageWindowMinutes", retention.archiveImmutableStageWindowMinutes);
        setFieldValue(archive, "maximumFuturePhenomenonSkewMinutes", retention.archiveMaximumFutureSkewMinutes);
        setFieldValue(archive, "parquetPrimaryIntervalMinutes", retention.archiveParquetPrimaryIntervalMinutes);
        setFieldValue(archive, "maximumStagePartitionsPerCycle", retention.archiveMaximumStagePartitionsPerCycle);
        setFieldValue(archive, "intervalMinutes", retention.archiveIntervalMinutes);
        setFieldValue(archive, "maximumGenerationsPerCycle", retention.archiveMaximumGenerationsPerCycle);
        setFieldValue(archive, "maximumVerificationsPerCycle", retention.archiveMaximumVerificationsPerCycle);
        setFieldValue(archive, "maximumDeletionsPerCycle", retention.archiveMaximumDeletionsPerCycle);
        setFieldValue(archive, "maximumCycleMinutes", retention.archiveMaximumCycleMinutes);
        setFieldValue(archive, "rollbackGraceDays", retention.archiveRollbackGraceDays);
        setFieldValue(archive, "deleteHotAfterArchive", retention.archiveDeleteHotAfterArchive);
    }

    private static Object getFieldValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getField(fieldName);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getField(fieldName);
            field.set(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumFieldValue(Object target, String fieldName, String enumConstant) {
        if (enumConstant == null || enumConstant.isBlank())
            return;
        try {
            Field field = target.getClass().getField(fieldName);
            Class<?> type = field.getType();
            if (type.isEnum()) {
                field.set(target, Enum.valueOf((Class<Enum>) type, enumConstant.trim().toUpperCase()));
            }
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
        }
    }

    public void loadModules(Collection<LaneConfig> laneConfigs) throws SensorHubException {
        // Filter out lanes with existing UIDs
        var existingLanes = reg.getLoadedModules(LaneSystem.class);
        var newLanes = laneConfigs.stream()
                .filter(laneConfig ->
                        existingLanes.stream().noneMatch(existingLane ->
                                Objects.equals(existingLane.getConfiguration().uniqueID, laneConfig.uniqueID)
                        )
                )
                .toList();

        for (var config : newLanes) {
            try {
                reg.loadModuleAsync(config, (event) -> {
                    if (event instanceof ModuleEvent moduleEvent) {
                        if (moduleEvent.getType() == ModuleEvent.Type.ERROR) {
                            if (moduleEvent.getError() != null) {
                                if (moduleEvent.getModule() != null && moduleEvent.getModule().getName() != null) {
                                    logger.warn("Could not import module: {}", moduleEvent.getModule().getName(), moduleEvent.getError());
                                } else {
                                    logger.warn("Could not import module", moduleEvent.getError());
                                }
                            } else {
                                logger.warn("Could not import module");
                            }
                        }
                    }
                });
            } catch (SensorHubException e) {
                logger.warn("Could not import module {}", config.name);
            }
        }

        logger.info("{} new lanes were loaded", newLanes.size());
    }

}
