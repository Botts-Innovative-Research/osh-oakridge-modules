package com.botts.impl.service.oscar.spreadsheet;

import com.botts.impl.service.oscar.OSCARServiceConfig;
import com.botts.impl.service.oscar.retention.StoragePressureRetentionConfig;
import com.botts.impl.service.oscar.video.VideoRetentionConfig;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.*;
import org.sensorhub.api.sensor.PositionConfig;

import java.io.*;
import java.util.*;

public class SpreadsheetParser {

    public static class RetentionPolicyConfig {
        public int timeToRetentionDays = 0;
        public int videoQueryPeriodMinutes = 1;
        public int maximumCatchUpHours = 6;
        public int lateArrivalOverlapHours = 24;
        public int lateArrivalScanPeriodMinutes = 360;
        public int lateArrivalCatchUpHours = 2;
        public int reconciliationCatchUpHours = 24;
        public int reconciliationPeriodHours = 24;
        public int occupancyStreamRefreshMinutes = 15;
        public int maintenanceWorkLimit = 200;
        public int metadataReclamationWorkLimit = 200;
        public boolean enableFrameRetention = true;
        public int frameRetentionCount = 5;
        public int jpegQuality = 85;
        public int sourceRollbackGraceHours = 24;
        public boolean enableSecondaryFramePruning = true;
        public int secondaryFrameRetentionDays = 30;

        public int storagePressureTriggerUsagePercent = 85;
        public int storagePressureTargetUsagePercent = 80;
        public int storagePressureCheckPeriodMinutes = 1;
        public int storagePressureMinimumObjectAgeMinutes = 10;
        public int storagePressureMaximumMediaSetsPerCycle = 200;
        public String storagePressureStoragePath = "files";

        public boolean delegateObservationLifecycleToDatabase = true;
        public int dailyFilePurgeWorkLimit = 50;
        public int connectionStatusRetentionHours = 1;

        public String dailyFileObservationMode = "LATEST";
        public int sealedJournalRetentionDays = 30;
        public boolean requireBackupBeforeSealedJournalDeletion = false;

        public boolean archiveEnabled = true;
        public String archiveStorageMode = "PARQUET_PRIMARY";
        public int archiveHotRetentionDays = 2;
        public int archiveParquetPrimaryDelayMinutes = 5;
        public int archiveImmutableStageWindowMinutes = 30;
        public int archiveMaximumFutureSkewMinutes = 5;
        public int archiveParquetPrimaryIntervalMinutes = 1;
        public int archiveMaximumStagePartitionsPerCycle = 8;
        public int archiveIntervalMinutes = 60;
        public int archiveMaximumGenerationsPerCycle = 2;
        public int archiveMaximumVerificationsPerCycle = 2;
        public int archiveMaximumDeletionsPerCycle = 2;
        public int archiveMaximumCycleMinutes = 15;
        public int archiveRollbackGraceDays = 3;
        public boolean archiveDeleteHotAfterArchive = true;

        public void applyRow(Map<String, String> row) {
            if (row == null)
                return;
            for (var entry : row.entrySet()) {
                applyKeyValue(entry.getKey(), entry.getValue());
            }
        }

        public void applyKeyValue(String key, String rawValue) {
            if (key == null || rawValue == null || rawValue.isBlank())
                return;
            String value = rawValue.trim();
            switch (key.trim()) {
                case SchemaV1.TIME_TO_RETENTION_DAYS, "timeToRetention" ->
                        timeToRetentionDays = Integer.parseInt(value);
                case SchemaV1.VIDEO_QUERY_PERIOD_MINUTES, "videoQueryPeriod" ->
                        videoQueryPeriodMinutes = Integer.parseInt(value);
                case SchemaV1.MAXIMUM_CATCH_UP_HOURS, "maximumCatchUpHours" ->
                        maximumCatchUpHours = Integer.parseInt(value);
                case SchemaV1.LATE_ARRIVAL_OVERLAP_HOURS, "lateArrivalOverlapHours" ->
                        lateArrivalOverlapHours = Integer.parseInt(value);
                case SchemaV1.LATE_ARRIVAL_SCAN_PERIOD_MINUTES, "lateArrivalScanPeriodMinutes" ->
                        lateArrivalScanPeriodMinutes = Integer.parseInt(value);
                case SchemaV1.LATE_ARRIVAL_CATCH_UP_HOURS, "lateArrivalCatchUpHours" ->
                        lateArrivalCatchUpHours = Integer.parseInt(value);
                case SchemaV1.RECONCILIATION_CATCH_UP_HOURS, "reconciliationCatchUpHours" ->
                        reconciliationCatchUpHours = Integer.parseInt(value);
                case SchemaV1.RECONCILIATION_PERIOD_HOURS, "reconciliationPeriodHours" ->
                        reconciliationPeriodHours = Integer.parseInt(value);
                case SchemaV1.OCCUPANCY_STREAM_REFRESH_MINUTES, "occupancyStreamRefreshMinutes" ->
                        occupancyStreamRefreshMinutes = Integer.parseInt(value);
                case SchemaV1.MAINTENANCE_WORK_LIMIT, "maintenanceWorkLimit" ->
                        maintenanceWorkLimit = Integer.parseInt(value);
                case SchemaV1.METADATA_RECLAMATION_WORK_LIMIT, "metadataReclamationWorkLimit" ->
                        metadataReclamationWorkLimit = Integer.parseInt(value);
                case SchemaV1.ENABLE_FRAME_RETENTION, "enableFrameRetention" ->
                        enableFrameRetention = Boolean.parseBoolean(value);
                case SchemaV1.FRAME_RETENTION_COUNT, "frameRetentionCount" ->
                        frameRetentionCount = Integer.parseInt(value);
                case SchemaV1.JPEG_QUALITY, "jpegQuality" ->
                        jpegQuality = Integer.parseInt(value);
                case SchemaV1.SOURCE_ROLLBACK_GRACE_HOURS, "sourceRollbackGraceHours" ->
                        sourceRollbackGraceHours = Integer.parseInt(value);
                case SchemaV1.ENABLE_SECONDARY_FRAME_PRUNING, "enableSecondaryFramePruning" ->
                        enableSecondaryFramePruning = Boolean.parseBoolean(value);
                case SchemaV1.SECONDARY_FRAME_RETENTION_DAYS, "secondaryFrameRetentionDays" ->
                        secondaryFrameRetentionDays = Integer.parseInt(value);
                case SchemaV1.STORAGE_PRESSURE_TRIGGER_USAGE_PERCENT, "triggerUsagePercent" ->
                        storagePressureTriggerUsagePercent = Integer.parseInt(value);
                case SchemaV1.STORAGE_PRESSURE_TARGET_USAGE_PERCENT, "targetUsagePercent" ->
                        storagePressureTargetUsagePercent = Integer.parseInt(value);
                case SchemaV1.STORAGE_PRESSURE_CHECK_PERIOD_MINUTES, "checkPeriodMinutes" ->
                        storagePressureCheckPeriodMinutes = Integer.parseInt(value);
                case SchemaV1.STORAGE_PRESSURE_MINIMUM_OBJECT_AGE_MINUTES, "minimumObjectAgeMinutes" ->
                        storagePressureMinimumObjectAgeMinutes = Integer.parseInt(value);
                case SchemaV1.STORAGE_PRESSURE_MAXIMUM_MEDIA_SETS_PER_CYCLE, "maximumMediaSetsPerCycle" ->
                        storagePressureMaximumMediaSetsPerCycle = Integer.parseInt(value);
                case SchemaV1.STORAGE_PRESSURE_STORAGE_PATH, "storagePath" ->
                        storagePressureStoragePath = value;
                case SchemaV1.DELEGATE_OBSERVATION_LIFECYCLE_TO_DATABASE, "delegateObservationLifecycleToDatabase" ->
                        delegateObservationLifecycleToDatabase = Boolean.parseBoolean(value);
                case SchemaV1.DAILY_FILE_PURGE_WORK_LIMIT, "dailyFilePurgeWorkLimit" ->
                        dailyFilePurgeWorkLimit = Integer.parseInt(value);
                case SchemaV1.CONNECTION_STATUS_RETENTION_HOURS, "connectionStatusRetentionHours" ->
                        connectionStatusRetentionHours = Integer.parseInt(value);
                case SchemaV1.DAILY_FILE_OBSERVATION_MODE, "dailyFileObservationMode" ->
                        dailyFileObservationMode = value;
                case SchemaV1.SEALED_JOURNAL_RETENTION_DAYS, "sealedJournalRetentionDays" ->
                        sealedJournalRetentionDays = Integer.parseInt(value);
                case SchemaV1.REQUIRE_BACKUP_BEFORE_SEALED_JOURNAL_DELETION, "requireBackupBeforeSealedJournalDeletion" ->
                        requireBackupBeforeSealedJournalDeletion = Boolean.parseBoolean(value);
                case SchemaV1.ARCHIVE_ENABLED, "archiveEnabled" ->
                        archiveEnabled = Boolean.parseBoolean(value);
                case SchemaV1.ARCHIVE_STORAGE_MODE, "archiveStorageMode" ->
                        archiveStorageMode = value;
                case SchemaV1.ARCHIVE_HOT_RETENTION_DAYS, "hotRetentionDays" ->
                        archiveHotRetentionDays = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_PARQUET_PRIMARY_DELAY_MINUTES, "parquetPrimaryArchiveDelayMinutes" ->
                        archiveParquetPrimaryDelayMinutes = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_IMMUTABLE_STAGE_WINDOW_MINUTES, "immutableStageWindowMinutes" ->
                        archiveImmutableStageWindowMinutes = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_MAXIMUM_FUTURE_SKEW_MINUTES, "maximumFuturePhenomenonSkewMinutes" ->
                        archiveMaximumFutureSkewMinutes = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_PARQUET_PRIMARY_INTERVAL_MINUTES, "parquetPrimaryIntervalMinutes" ->
                        archiveParquetPrimaryIntervalMinutes = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_MAXIMUM_STAGE_PARTITIONS_PER_CYCLE, "maximumStagePartitionsPerCycle" ->
                        archiveMaximumStagePartitionsPerCycle = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_INTERVAL_MINUTES, "intervalMinutes" ->
                        archiveIntervalMinutes = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_MAXIMUM_GENERATIONS_PER_CYCLE, "maximumGenerationsPerCycle" ->
                        archiveMaximumGenerationsPerCycle = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_MAXIMUM_VERIFICATIONS_PER_CYCLE, "maximumVerificationsPerCycle" ->
                        archiveMaximumVerificationsPerCycle = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_MAXIMUM_DELETIONS_PER_CYCLE, "maximumDeletionsPerCycle" ->
                        archiveMaximumDeletionsPerCycle = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_MAXIMUM_CYCLE_MINUTES, "maximumCycleMinutes" ->
                        archiveMaximumCycleMinutes = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_ROLLBACK_GRACE_DAYS, "rollbackGraceDays" ->
                        archiveRollbackGraceDays = Integer.parseInt(value);
                case SchemaV1.ARCHIVE_DELETE_HOT_AFTER_ARCHIVE, "deleteHotAfterArchive" ->
                        archiveDeleteHotAfterArchive = Boolean.parseBoolean(value);
                default -> {
                    // Non-retention column
                }
            }
        }

        public void applyToOscarConfig(OSCARServiceConfig oscarConfig) {
            if (oscarConfig == null)
                return;
            if (oscarConfig.videoRetentionConfig == null)
                oscarConfig.videoRetentionConfig = new VideoRetentionConfig();
            oscarConfig.videoRetentionConfig.timeToRetention = timeToRetentionDays;
            oscarConfig.videoRetentionConfig.videoQueryPeriod = videoQueryPeriodMinutes;
            oscarConfig.videoRetentionConfig.maximumCatchUpHours = maximumCatchUpHours;
            oscarConfig.videoRetentionConfig.lateArrivalOverlapHours = lateArrivalOverlapHours;
            oscarConfig.videoRetentionConfig.lateArrivalScanPeriodMinutes = lateArrivalScanPeriodMinutes;
            oscarConfig.videoRetentionConfig.lateArrivalCatchUpHours = lateArrivalCatchUpHours;
            oscarConfig.videoRetentionConfig.reconciliationCatchUpHours = reconciliationCatchUpHours;
            oscarConfig.videoRetentionConfig.reconciliationPeriodHours = reconciliationPeriodHours;
            oscarConfig.videoRetentionConfig.occupancyStreamRefreshMinutes = occupancyStreamRefreshMinutes;
            oscarConfig.videoRetentionConfig.maintenanceWorkLimit = maintenanceWorkLimit;
            oscarConfig.videoRetentionConfig.metadataReclamationWorkLimit = metadataReclamationWorkLimit;
            oscarConfig.videoRetentionConfig.enableFrameRetention = enableFrameRetention;
            oscarConfig.videoRetentionConfig.frameRetentionCount = frameRetentionCount;
            oscarConfig.videoRetentionConfig.jpegQuality = jpegQuality;
            oscarConfig.videoRetentionConfig.sourceRollbackGraceHours = sourceRollbackGraceHours;
            oscarConfig.videoRetentionConfig.enableSecondaryFramePruning = enableSecondaryFramePruning;
            oscarConfig.videoRetentionConfig.secondaryFrameRetentionDays = secondaryFrameRetentionDays;

            if (oscarConfig.storagePressureRetentionConfig == null)
                oscarConfig.storagePressureRetentionConfig = new StoragePressureRetentionConfig();
            oscarConfig.storagePressureRetentionConfig.triggerUsagePercent = storagePressureTriggerUsagePercent;
            oscarConfig.storagePressureRetentionConfig.targetUsagePercent = storagePressureTargetUsagePercent;
            oscarConfig.storagePressureRetentionConfig.checkPeriodMinutes = storagePressureCheckPeriodMinutes;
            oscarConfig.storagePressureRetentionConfig.minimumObjectAgeMinutes = storagePressureMinimumObjectAgeMinutes;
            oscarConfig.storagePressureRetentionConfig.maximumMediaSetsPerCycle = storagePressureMaximumMediaSetsPerCycle;
            oscarConfig.storagePressureRetentionConfig.storagePath = storagePressureStoragePath;

            oscarConfig.delegateObservationLifecycleToDatabase = delegateObservationLifecycleToDatabase;
            oscarConfig.dailyFilePurgeWorkLimit = dailyFilePurgeWorkLimit;
            oscarConfig.connectionStatusRetentionHours = connectionStatusRetentionHours;
        }

        public void readFromOscarConfig(OSCARServiceConfig oscarConfig) {
            if (oscarConfig == null)
                return;
            if (oscarConfig.videoRetentionConfig != null) {
                VideoRetentionConfig v = oscarConfig.videoRetentionConfig;
                timeToRetentionDays = v.timeToRetention;
                videoQueryPeriodMinutes = v.videoQueryPeriod;
                maximumCatchUpHours = v.maximumCatchUpHours;
                lateArrivalOverlapHours = v.lateArrivalOverlapHours;
                lateArrivalScanPeriodMinutes = v.lateArrivalScanPeriodMinutes;
                lateArrivalCatchUpHours = v.lateArrivalCatchUpHours;
                reconciliationCatchUpHours = v.reconciliationCatchUpHours;
                reconciliationPeriodHours = v.reconciliationPeriodHours;
                occupancyStreamRefreshMinutes = v.occupancyStreamRefreshMinutes;
                maintenanceWorkLimit = v.maintenanceWorkLimit;
                metadataReclamationWorkLimit = v.metadataReclamationWorkLimit;
                enableFrameRetention = v.enableFrameRetention;
                frameRetentionCount = v.frameRetentionCount;
                jpegQuality = v.jpegQuality;
                sourceRollbackGraceHours = v.sourceRollbackGraceHours;
                enableSecondaryFramePruning = v.enableSecondaryFramePruning;
                secondaryFrameRetentionDays = v.secondaryFrameRetentionDays;
            }
            if (oscarConfig.storagePressureRetentionConfig != null) {
                StoragePressureRetentionConfig s = oscarConfig.storagePressureRetentionConfig;
                storagePressureTriggerUsagePercent = s.triggerUsagePercent;
                storagePressureTargetUsagePercent = s.targetUsagePercent;
                storagePressureCheckPeriodMinutes = s.checkPeriodMinutes;
                storagePressureMinimumObjectAgeMinutes = s.minimumObjectAgeMinutes;
                storagePressureMaximumMediaSetsPerCycle = s.maximumMediaSetsPerCycle;
                if (s.storagePath != null && !s.storagePath.isBlank())
                    storagePressureStoragePath = s.storagePath;
            }
            delegateObservationLifecycleToDatabase = oscarConfig.delegateObservationLifecycleToDatabase;
            dailyFilePurgeWorkLimit = oscarConfig.dailyFilePurgeWorkLimit;
            connectionStatusRetentionHours = oscarConfig.connectionStatusRetentionHours;
        }
    }

    SchemaV1 schema;
    String[] headers;
    String DELIMITER = ",";
    private RetentionPolicyConfig retentionPolicyConfig = new RetentionPolicyConfig();

    public SpreadsheetParser() {
        schema = new SchemaV1();
    }

    public RetentionPolicyConfig getRetentionPolicyConfig() {
        return retentionPolicyConfig;
    }

    public void setRetentionPolicyConfig(RetentionPolicyConfig retentionPolicyConfig) {
        this.retentionPolicyConfig = Objects.requireNonNull(retentionPolicyConfig, "retentionPolicyConfig");
    }

    public void validateHeader(String headerLine) throws IllegalArgumentException {
        if (headerLine == null)
            throw new IllegalArgumentException("Spreadsheet must include a header row");

        String[] values = headerLine.split(DELIMITER, -1);

        boolean hasOperationalViews = values.length > 5 && Objects.equals(values[5], SchemaV1.OPERATIONAL_VIEWS);
        String[] mainHeaders;
        if (!hasOperationalViews) {
            mainHeaders = SchemaV1.LEGACY_MAIN_HEADERS;
        } else if (values.length > SchemaV1.V3_10_MAIN_HEADERS.length
                && Objects.equals(values[SchemaV1.V3_10_MAIN_HEADERS.length], SchemaV1.CAMERA_TYPE_0)) {
            mainHeaders = SchemaV1.V3_10_MAIN_HEADERS;
        } else {
            mainHeaders = SchemaV1.MAIN_HEADERS;
        }

        int cameraColumnCount = values.length - mainHeaders.length;

        // Ensure extra headers in groups of 6
        if (cameraColumnCount < SchemaV1.CAMERA_HEADERS.length
                || cameraColumnCount % SchemaV1.CAMERA_HEADERS.length != 0) {
            throw new IllegalArgumentException(
                    "Extra cameras must be configured as " +
                            "[CameraTypeX, CameraHostX, CameraPathX, CodecX, UsernameX, PasswordX] " +
                            "with X sequential starting at 0"
            );
        }

        // Validate fixed headers
        for (int i = 0; i < mainHeaders.length; i++) {
            String current = values[i];
            String expected = mainHeaders[i];
            if (!Objects.equals(current, expected)) {
                throw new IllegalArgumentException("Expected: " + expected + " Got: " + current);
            }
        }

        // Validate camera headers
        int numCameraGroups = cameraColumnCount / SchemaV1.CAMERA_HEADERS.length;
        for (int camIndex = 0; camIndex < numCameraGroups; camIndex++) {
            for (int j = 0; j < SchemaV1.CAMERA_HEADERS.length; j++) {
                String expected = SchemaV1.CAMERA_HEADERS[j].replace("0", String.valueOf(camIndex));
                String actual = values[mainHeaders.length + camIndex * SchemaV1.CAMERA_HEADERS.length + j];
                if (!Objects.equals(expected, actual)) {
                    throw new IllegalArgumentException(
                            "Expected camera header: " + expected + " Got: " + actual
                    );
                }
            }
        }

        headers = values;
    }

    public Map<String, String> toMap(String line) {
        String[] values = line.split(DELIMITER, -1);
        Map<String, String> result = new LinkedHashMap<>();

        if (values.length != headers.length)
            throw new IllegalArgumentException("Values must match headers");

        for (int i = 0; i < values.length; i++)
            result.put(headers[i], values[i].trim());

        return result;
    }

    public List<LaneConfig> deserialize(String csv) throws IOException {
        List<LaneConfig> lanes = new ArrayList<>();
        retentionPolicyConfig = new RetentionPolicyConfig();
        try (BufferedReader reader = new BufferedReader(new StringReader(csv))) {
            String headerLine = null;
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty())
                    continue;
                if (trimmed.startsWith("#RETENTION,") || trimmed.startsWith("RetentionPolicy,")) {
                    String[] parts = trimmed.split(DELIMITER, 3);
                    if (parts.length >= 3)
                        retentionPolicyConfig.applyKeyValue(parts[1], parts[2]);
                    continue;
                }
                if (trimmed.startsWith("#"))
                    continue;
                if (headerLine == null) {
                    headerLine = line;
                    validateHeader(headerLine);
                    continue;
                }

                Map<String, String> row = toMap(line);
                retentionPolicyConfig.applyRow(row);
                LaneConfig laneConfig = deserializeLane(row);
                lanes.add(laneConfig);
            }
            if (headerLine == null)
                validateHeader(null);
        }
        return lanes;
    }

    public LaneConfig deserializeLane(Map<String, String> row) {

        LaneConfig laneConfig = new LaneConfig();
        laneConfig.moduleClass = LaneSystem.class.getCanonicalName();
        laneConfig.id = UUID.randomUUID().toString();
        laneConfig.name = row.get(SchemaV1.NAME);
        laneConfig.uniqueID = row.get(SchemaV1.UID);
        laneConfig.autoStart = Boolean.parseBoolean(row.get(SchemaV1.AUTO_START));

        String autoDelete = row.get(SchemaV1.AUTO_DELETE);
        if (autoDelete != null && !autoDelete.isBlank())
            laneConfig.autoDelete = Boolean.parseBoolean(autoDelete);

        String operationalViews = row.get(SchemaV1.OPERATIONAL_VIEWS);
        if (operationalViews != null && !operationalViews.isBlank()) {
            laneConfig.operationalViewKeys = Arrays.stream(operationalViews.split(";"))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }

        if (!row.get(SchemaV1.LATITUDE).isBlank() && !row.get(SchemaV1.LONGITUDE).isBlank()) {
            PositionConfig.LLALocation location = new PositionConfig.LLALocation();
            location.lat = Double.parseDouble(row.get(SchemaV1.LATITUDE));
            location.lon = Double.parseDouble(row.get(SchemaV1.LONGITUDE));
            laneConfig.location = location;
        }

        laneConfig.laneOptionsConfig = new LaneOptionsConfig();

        var rpmType = row.get(SchemaV1.RPM_TYPE);
        RPMConfig rpmConfig = null;
        if (!rpmType.isBlank())
            rpmConfig = deserializeRPM(row);

        int camIndex = 0;
        ArrayList<FFMpegConfig> ffMpegConfigs = new ArrayList<>();
        while (row.containsKey("CameraType" + camIndex)) {
            String cameraType = row.get("CameraType" + camIndex);
            if (cameraType.isBlank()) {
                camIndex++;
                continue;
            }

            FFMpegConfig ffMpegConfig = deserializeCamera(row, camIndex++);
            if (ffMpegConfig != null)
                ffMpegConfigs.add(ffMpegConfig);
        }

        if (rpmConfig != null)
            laneConfig.laneOptionsConfig.rpmConfig = rpmConfig;
        if (!ffMpegConfigs.isEmpty())
            laneConfig.laneOptionsConfig.ffmpegConfig = ffMpegConfigs;
        return laneConfig;

    }

    public RPMConfig deserializeRPM(Map<String, String> row) {
        RPMConfig rpmConfig = null;

        var rpmType = row.get(SchemaV1.RPM_TYPE);
        if (rpmType.equalsIgnoreCase("Aspect")) {
            rpmConfig = new AspectRPMConfig();
            rpmConfig.remoteHost = row.get(SchemaV1.RPM_HOST);
            rpmConfig.remotePort = Integer.parseInt(row.get(SchemaV1.RPM_PORT));
            ((AspectRPMConfig) rpmConfig).addressRange.from = Integer.parseInt(row.get(SchemaV1.ASPECT_ADDRESS_START));
            ((AspectRPMConfig) rpmConfig).addressRange.to = Integer.parseInt(row.get(SchemaV1.ASPECT_ADDRESS_END));
        } else if (rpmType.equalsIgnoreCase("Rapiscan")) {
            rpmConfig  = new RapiscanRPMConfig();
            rpmConfig.remoteHost = row.get(SchemaV1.RPM_HOST);
            rpmConfig.remotePort = Integer.parseInt(row.get(SchemaV1.RPM_PORT));
            var eml = ((RapiscanRPMConfig) rpmConfig).emlConfig = new RapiscanRPMConfig.EMLConfig();
            eml.emlEnabled = Boolean.parseBoolean(row.get(SchemaV1.EML_ENABLED));
            eml.isCollimated = row.get(SchemaV1.EML_COLLIMATED) != null && Boolean.parseBoolean(row.get(SchemaV1.EML_COLLIMATED));
            eml.laneWidth = row.get(SchemaV1.LANE_WIDTH) == null ? 0.0f : Double.parseDouble(row.get(SchemaV1.LANE_WIDTH));
        } else if (rpmType.equalsIgnoreCase("RS350")) {
            rpmConfig = new RS350RPMConfig();
            rpmConfig.remoteHost = row.get(SchemaV1.RPM_HOST);
            rpmConfig.remotePort = Integer.parseInt(row.get(SchemaV1.RPM_PORT));
        }

        return rpmConfig;
    }

    public FFMpegConfig deserializeCamera(Map<String, String> row, int camIndex) {
        FFMpegConfig ffMpegConfig = null;

        String cameraType = row.get(SchemaV1.CAMERA_TYPE + camIndex);
        if (cameraType == null)
            return null;

        if (cameraType.equalsIgnoreCase("Custom")) {
            ffMpegConfig = new CustomCameraConfig();
            ffMpegConfig.password = row.get(SchemaV1.CAMERA_PASSWORD + camIndex) == null ? "" : row.get(SchemaV1.CAMERA_PASSWORD + camIndex);
            ffMpegConfig.username = row.get(SchemaV1.CAMERA_USERNAME + camIndex) == null ? "" :row.get(SchemaV1.CAMERA_USERNAME + camIndex);
            ((CustomCameraConfig)ffMpegConfig).streamPath = row.get(SchemaV1.CAMERA_PATH + camIndex);
        } else if (cameraType.equalsIgnoreCase("Axis")) {
            ffMpegConfig = new AxisCameraConfig();
            ffMpegConfig.username = row.get(SchemaV1.CAMERA_USERNAME + camIndex) == null ? "" :row.get(SchemaV1.CAMERA_USERNAME + camIndex);
            ffMpegConfig.password = row.get(SchemaV1.CAMERA_PASSWORD + camIndex) == null ? "" : row.get(SchemaV1.CAMERA_PASSWORD + camIndex);
            var codec = row.get(SchemaV1.CODEC + camIndex);
            if (codec == null || (codec.equalsIgnoreCase("h264") || codec.equalsIgnoreCase("h.264")))
                ((AxisCameraConfig) ffMpegConfig).streamPath = AxisCameraConfig.CodecEndpoint.H264;
            else if (codec.equalsIgnoreCase("mjpeg") || codec.equalsIgnoreCase("jpeg"))
                ((AxisCameraConfig) ffMpegConfig).streamPath = AxisCameraConfig.CodecEndpoint.MJPEG;
        } else if (cameraType.equalsIgnoreCase("Sony")) {
            ffMpegConfig = new SonyCameraConfig();
            ffMpegConfig.username = row.get(SchemaV1.CAMERA_USERNAME + camIndex);
            ffMpegConfig.password = row.get(SchemaV1.CAMERA_PASSWORD + camIndex);
        }

        if (ffMpegConfig == null)
            return null;

        String host = row.get(SchemaV1.CAMERA_HOST + camIndex);
        if (host != null)
            ffMpegConfig.remoteHost = host;
        else
            throw new IllegalArgumentException("Must specify camera host address");

        return ffMpegConfig;
    }


    public String serialize(Collection<LaneConfig> lanes) {
        int maxCameras = Math.max(1, lanes.stream()
                .mapToInt(lane -> lane.laneOptionsConfig == null || lane.laneOptionsConfig.ffmpegConfig == null
                        ? 0
                        : lane.laneOptionsConfig.ffmpegConfig.size())
                .max()
                .orElse(0));

        StringBuilder builder = new StringBuilder()
                .append(String.join(DELIMITER, schema.getHeaders(maxCameras)))
                .append("\n");

        lanes.forEach(lane -> {
                builder.append(serializeLane(lane, maxCameras));
                builder.append("\n");
        });

        return builder.toString();
    }

    public String serializeLane(LaneConfig lane, int maxCameras) {
        List<String> r = new LinkedList<>();
        // Lane stuff
        addToRow(r, lane.name);
        addToRow(r, lane.uniqueID);
        addToRow(r, lane.autoStart);

        var loc = lane.location;
        if (loc != null) {
            addToRow(r, loc.lat);
            addToRow(r, loc.lon);
        } else
            addNullsToRow(r, 2);

        addToRow(r, String.join(";", lane.operationalViewKeys == null ? List.of() : lane.operationalViewKeys));

        var opts = lane.laneOptionsConfig;
        var rpm = opts == null ? null : opts.rpmConfig;
        List<FFMpegConfig> cameras = opts == null || opts.ffmpegConfig == null
                ? List.of()
                : opts.ffmpegConfig;
        if (rpm != null) {
            if (rpm instanceof AspectRPMConfig aspect) {
                addToRow(r, "Aspect");
                addToRow(r, aspect.remoteHost);
                addToRow(r, aspect.remotePort);
                addToRow(r, aspect.addressRange.from);
                addToRow(r, aspect.addressRange.to);
                // Add empty vals for EML
                addNullsToRow(r, 3);
            } else if (rpm instanceof RapiscanRPMConfig rapiscan) {
                addToRow(r, "Rapiscan");
                addToRow(r, rapiscan.remoteHost);
                addToRow(r, rapiscan.remotePort);
                // Aspect start & end
                addNullsToRow(r, 2);
                var eml = rapiscan.emlConfig;
                if (eml != null) {
                    addToRow(r, eml.emlEnabled);
                    addToRow(r, eml.isCollimated);
                    addToRow(r, eml.laneWidth);
                } else
                    addNullsToRow(r, 3);
            } else if (rpm instanceof RS350RPMConfig rs350) {
                addToRow(r, "RS350");
                addToRow(r, rs350.remoteHost);
                addToRow(r, rs350.remotePort);
                // Aspect start/end and EML columns
                addNullsToRow(r, 5);
            }
        } else {
            addNullsToRow(r, 8);
        }

        // Lane and node retention policies
        addToRow(r, lane.autoDelete);
        RetentionPolicyConfig retention = retentionPolicyConfig != null
                ? retentionPolicyConfig : new RetentionPolicyConfig();
        addToRow(r, retention.timeToRetentionDays);
        addToRow(r, retention.videoQueryPeriodMinutes);
        addToRow(r, retention.maximumCatchUpHours);
        addToRow(r, retention.lateArrivalOverlapHours);
        addToRow(r, retention.lateArrivalScanPeriodMinutes);
        addToRow(r, retention.lateArrivalCatchUpHours);
        addToRow(r, retention.reconciliationCatchUpHours);
        addToRow(r, retention.reconciliationPeriodHours);
        addToRow(r, retention.occupancyStreamRefreshMinutes);
        addToRow(r, retention.maintenanceWorkLimit);
        addToRow(r, retention.metadataReclamationWorkLimit);
        addToRow(r, retention.enableFrameRetention);
        addToRow(r, retention.frameRetentionCount);
        addToRow(r, retention.jpegQuality);
        addToRow(r, retention.sourceRollbackGraceHours);
        addToRow(r, retention.enableSecondaryFramePruning);
        addToRow(r, retention.secondaryFrameRetentionDays);
        addToRow(r, retention.storagePressureTriggerUsagePercent);
        addToRow(r, retention.storagePressureTargetUsagePercent);
        addToRow(r, retention.storagePressureCheckPeriodMinutes);
        addToRow(r, retention.storagePressureMinimumObjectAgeMinutes);
        addToRow(r, retention.storagePressureMaximumMediaSetsPerCycle);
        addToRow(r, retention.storagePressureStoragePath);
        addToRow(r, retention.delegateObservationLifecycleToDatabase);
        addToRow(r, retention.dailyFilePurgeWorkLimit);
        addToRow(r, retention.connectionStatusRetentionHours);
        addToRow(r, retention.dailyFileObservationMode);
        addToRow(r, retention.sealedJournalRetentionDays);
        addToRow(r, retention.requireBackupBeforeSealedJournalDeletion);
        addToRow(r, retention.archiveEnabled);
        addToRow(r, retention.archiveStorageMode);
        addToRow(r, retention.archiveHotRetentionDays);
        addToRow(r, retention.archiveParquetPrimaryDelayMinutes);
        addToRow(r, retention.archiveImmutableStageWindowMinutes);
        addToRow(r, retention.archiveMaximumFutureSkewMinutes);
        addToRow(r, retention.archiveParquetPrimaryIntervalMinutes);
        addToRow(r, retention.archiveMaximumStagePartitionsPerCycle);
        addToRow(r, retention.archiveIntervalMinutes);
        addToRow(r, retention.archiveMaximumGenerationsPerCycle);
        addToRow(r, retention.archiveMaximumVerificationsPerCycle);
        addToRow(r, retention.archiveMaximumDeletionsPerCycle);
        addToRow(r, retention.archiveMaximumCycleMinutes);
        addToRow(r, retention.archiveRollbackGraceDays);
        addToRow(r, retention.archiveDeleteHotAfterArchive);

        // Add header and vals for default camera
        for (int i = 0; i < maxCameras; i++) {
            if (cameras.size() > i) {
                var cam = cameras.get(i);

                if (cam instanceof CustomCameraConfig custom) {
                    addToRow(r, "Custom");
                    addToRow(r, custom.remoteHost);
                    addToRow(r, custom.streamPath);
                    addToRow(r, null);
                    addToRow(r, custom.username);
                    addToRow(r, custom.password);
                } else if (cam instanceof AxisCameraConfig axis) {
                    addToRow(r, "Axis");
                    addToRow(r, axis.remoteHost);
                    addToRow(r, null);
                    if (axis.streamPath == AxisCameraConfig.CodecEndpoint.H264) {
                        addToRow(r, "H264");
                    } else if (axis.streamPath == AxisCameraConfig.CodecEndpoint.MJPEG) {
                        addToRow(r, "MJPEG");
                    }
                    addToRow(r, axis.username);
                    addToRow(r, axis.password);
                } else if (cam instanceof SonyCameraConfig sony) {
                    addToRow(r, "Sony");
                    addToRow(r, sony.remoteHost);
                    addNullsToRow(r, 2);
                    addToRow(r, sony.username);
                    addToRow(r, sony.password);
                }
            } else {
                addNullsToRow(r, SchemaV1.CAMERA_HEADERS.length);
            }
        }

        return String.join(DELIMITER, r);
    }

    private void addToRow(List<String> row, Object item) {
        if (item != null)
            row.add(String.valueOf(item));
        else
            row.add("");
    }

    private void addNullsToRow(List<String> row, int numVals) {
        for (int i = 0; i < numVals; i++)
            addToRow(row, null);
    }
}
