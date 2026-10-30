package com.botts.impl.service.oscar.spreadsheet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SchemaV1 {

    // Lane stuff
    public static final String NAME = "Name";
    public static final String UID = "UniqueID";
    public static final String AUTO_START = "AutoStart";
    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";
    public static final String OPERATIONAL_VIEWS = "OperationalViews";

    // RPM stuff
    public static final String RPM_TYPE = "RPMConfigType";
    public static final String RPM_HOST = "RPMHost";
    public static final String RPM_PORT = "RPMPort";
    public static final String ASPECT_ADDRESS_START = "AspectAddressStart";
    public static final String ASPECT_ADDRESS_END = "AspectAddressEnd";
    public static final String EML_ENABLED = "EMLEnabled";
    public static final String EML_COLLIMATED = "EMLCollimated";
    public static final String LANE_WIDTH = "LaneWidth";
    public static final String AUTO_DELETE = "AutoDelete";

    // Retention policy columns (with .57 defaults)
    public static final String TIME_TO_RETENTION_DAYS = "TimeToRetentionDays";
    public static final String VIDEO_QUERY_PERIOD_MINUTES = "VideoQueryPeriodMinutes";
    public static final String MAXIMUM_CATCH_UP_HOURS = "MaximumCatchUpHours";
    public static final String LATE_ARRIVAL_OVERLAP_HOURS = "LateArrivalOverlapHours";
    public static final String LATE_ARRIVAL_SCAN_PERIOD_MINUTES = "LateArrivalScanPeriodMinutes";
    public static final String LATE_ARRIVAL_CATCH_UP_HOURS = "LateArrivalCatchUpHours";
    public static final String RECONCILIATION_CATCH_UP_HOURS = "ReconciliationCatchUpHours";
    public static final String RECONCILIATION_PERIOD_HOURS = "ReconciliationPeriodHours";
    public static final String OCCUPANCY_STREAM_REFRESH_MINUTES = "OccupancyStreamRefreshMinutes";
    public static final String MAINTENANCE_WORK_LIMIT = "MaintenanceWorkLimit";
    public static final String METADATA_RECLAMATION_WORK_LIMIT = "MetadataReclamationWorkLimit";
    public static final String ENABLE_FRAME_RETENTION = "EnableFrameRetention";
    public static final String FRAME_RETENTION_COUNT = "FrameRetentionCount";
    public static final String JPEG_QUALITY = "JpegQuality";
    public static final String SOURCE_ROLLBACK_GRACE_HOURS = "SourceRollbackGraceHours";
    public static final String ENABLE_SECONDARY_FRAME_PRUNING = "EnableSecondaryFramePruning";
    public static final String SECONDARY_FRAME_RETENTION_DAYS = "SecondaryFrameRetentionDays";
    public static final String STORAGE_PRESSURE_TRIGGER_USAGE_PERCENT = "StoragePressureTriggerUsagePercent";
    public static final String STORAGE_PRESSURE_TARGET_USAGE_PERCENT = "StoragePressureTargetUsagePercent";
    public static final String STORAGE_PRESSURE_CHECK_PERIOD_MINUTES = "StoragePressureCheckPeriodMinutes";
    public static final String STORAGE_PRESSURE_MINIMUM_OBJECT_AGE_MINUTES = "StoragePressureMinimumObjectAgeMinutes";
    public static final String STORAGE_PRESSURE_MAXIMUM_MEDIA_SETS_PER_CYCLE = "StoragePressureMaximumMediaSetsPerCycle";
    public static final String STORAGE_PRESSURE_STORAGE_PATH = "StoragePressureStoragePath";
    public static final String DELEGATE_OBSERVATION_LIFECYCLE_TO_DATABASE = "DelegateObservationLifecycleToDatabase";
    public static final String DAILY_FILE_PURGE_WORK_LIMIT = "DailyFilePurgeWorkLimit";
    public static final String CONNECTION_STATUS_RETENTION_HOURS = "ConnectionStatusRetentionHours";
    public static final String DAILY_FILE_OBSERVATION_MODE = "DailyFileObservationMode";
    public static final String SEALED_JOURNAL_RETENTION_DAYS = "SealedJournalRetentionDays";
    public static final String REQUIRE_BACKUP_BEFORE_SEALED_JOURNAL_DELETION = "RequireBackupBeforeSealedJournalDeletion";
    public static final String ARCHIVE_ENABLED = "ArchiveEnabled";
    public static final String ARCHIVE_STORAGE_MODE = "ArchiveStorageMode";
    public static final String ARCHIVE_HOT_RETENTION_DAYS = "ArchiveHotRetentionDays";
    public static final String ARCHIVE_PARQUET_PRIMARY_DELAY_MINUTES = "ArchiveParquetPrimaryDelayMinutes";
    public static final String ARCHIVE_IMMUTABLE_STAGE_WINDOW_MINUTES = "ArchiveImmutableStageWindowMinutes";
    public static final String ARCHIVE_MAXIMUM_FUTURE_SKEW_MINUTES = "ArchiveMaximumFutureSkewMinutes";
    public static final String ARCHIVE_PARQUET_PRIMARY_INTERVAL_MINUTES = "ArchiveParquetPrimaryIntervalMinutes";
    public static final String ARCHIVE_MAXIMUM_STAGE_PARTITIONS_PER_CYCLE = "ArchiveMaximumStagePartitionsPerCycle";
    public static final String ARCHIVE_INTERVAL_MINUTES = "ArchiveIntervalMinutes";
    public static final String ARCHIVE_MAXIMUM_GENERATIONS_PER_CYCLE = "ArchiveMaximumGenerationsPerCycle";
    public static final String ARCHIVE_MAXIMUM_VERIFICATIONS_PER_CYCLE = "ArchiveMaximumVerificationsPerCycle";
    public static final String ARCHIVE_MAXIMUM_DELETIONS_PER_CYCLE = "ArchiveMaximumDeletionsPerCycle";
    public static final String ARCHIVE_MAXIMUM_CYCLE_MINUTES = "ArchiveMaximumCycleMinutes";
    public static final String ARCHIVE_ROLLBACK_GRACE_DAYS = "ArchiveRollbackGraceDays";
    public static final String ARCHIVE_DELETE_HOT_AFTER_ARCHIVE = "ArchiveDeleteHotAfterArchive";

    // Camera stuff
    public static final String CAMERA_TYPE = "CameraType";
    public static final String CAMERA_HOST = "CameraHost";
    public static final String CAMERA_PATH = "CameraPath";
    public static final String CODEC = "Codec";
    public static final String CAMERA_USERNAME = "Username";
    public static final String CAMERA_PASSWORD = "Password";

    public static final String CAMERA_TYPE_0 = "CameraType0";
    public static final String CAMERA_HOST_0 = "CameraHost0";
    public static final String CAMERA_PATH_0 = "CameraPath0";
    public static final String CODEC_0 = "Codec0";
    public static final String CAMERA_USERNAME_0 = "Username0";
    public static final String CAMERA_PASSWORD_0 = "Password0";

    public static final String[] LEGACY_MAIN_HEADERS = {
            "Name",
            "UniqueID",
            "AutoStart",
            "Latitude",
            "Longitude",
            "RPMConfigType",
            "RPMHost",
            "RPMPort",
            "AspectAddressStart",
            "AspectAddressEnd",
            "EMLEnabled",
            "EMLCollimated",
            "LaneWidth"
    };

    public static final String[] V3_10_MAIN_HEADERS = {
            "Name",
            "UniqueID",
            "AutoStart",
            "Latitude",
            "Longitude",
            "OperationalViews",
            "RPMConfigType",
            "RPMHost",
            "RPMPort",
            "AspectAddressStart",
            "AspectAddressEnd",
            "EMLEnabled",
            "EMLCollimated",
            "LaneWidth"
    };

    public static final String[] MAIN_HEADERS = {
            "Name",
            "UniqueID",
            "AutoStart",
            "Latitude",
            "Longitude",
            "OperationalViews",
            "RPMConfigType",
            "RPMHost",
            "RPMPort",
            "AspectAddressStart",
            "AspectAddressEnd",
            "EMLEnabled",
            "EMLCollimated",
            "LaneWidth",
            AUTO_DELETE,
            TIME_TO_RETENTION_DAYS,
            VIDEO_QUERY_PERIOD_MINUTES,
            MAXIMUM_CATCH_UP_HOURS,
            LATE_ARRIVAL_OVERLAP_HOURS,
            LATE_ARRIVAL_SCAN_PERIOD_MINUTES,
            LATE_ARRIVAL_CATCH_UP_HOURS,
            RECONCILIATION_CATCH_UP_HOURS,
            RECONCILIATION_PERIOD_HOURS,
            OCCUPANCY_STREAM_REFRESH_MINUTES,
            MAINTENANCE_WORK_LIMIT,
            METADATA_RECLAMATION_WORK_LIMIT,
            ENABLE_FRAME_RETENTION,
            FRAME_RETENTION_COUNT,
            JPEG_QUALITY,
            SOURCE_ROLLBACK_GRACE_HOURS,
            ENABLE_SECONDARY_FRAME_PRUNING,
            SECONDARY_FRAME_RETENTION_DAYS,
            STORAGE_PRESSURE_TRIGGER_USAGE_PERCENT,
            STORAGE_PRESSURE_TARGET_USAGE_PERCENT,
            STORAGE_PRESSURE_CHECK_PERIOD_MINUTES,
            STORAGE_PRESSURE_MINIMUM_OBJECT_AGE_MINUTES,
            STORAGE_PRESSURE_MAXIMUM_MEDIA_SETS_PER_CYCLE,
            STORAGE_PRESSURE_STORAGE_PATH,
            DELEGATE_OBSERVATION_LIFECYCLE_TO_DATABASE,
            DAILY_FILE_PURGE_WORK_LIMIT,
            CONNECTION_STATUS_RETENTION_HOURS,
            DAILY_FILE_OBSERVATION_MODE,
            SEALED_JOURNAL_RETENTION_DAYS,
            REQUIRE_BACKUP_BEFORE_SEALED_JOURNAL_DELETION,
            ARCHIVE_ENABLED,
            ARCHIVE_STORAGE_MODE,
            ARCHIVE_HOT_RETENTION_DAYS,
            ARCHIVE_PARQUET_PRIMARY_DELAY_MINUTES,
            ARCHIVE_IMMUTABLE_STAGE_WINDOW_MINUTES,
            ARCHIVE_MAXIMUM_FUTURE_SKEW_MINUTES,
            ARCHIVE_PARQUET_PRIMARY_INTERVAL_MINUTES,
            ARCHIVE_MAXIMUM_STAGE_PARTITIONS_PER_CYCLE,
            ARCHIVE_INTERVAL_MINUTES,
            ARCHIVE_MAXIMUM_GENERATIONS_PER_CYCLE,
            ARCHIVE_MAXIMUM_VERIFICATIONS_PER_CYCLE,
            ARCHIVE_MAXIMUM_DELETIONS_PER_CYCLE,
            ARCHIVE_MAXIMUM_CYCLE_MINUTES,
            ARCHIVE_ROLLBACK_GRACE_DAYS,
            ARCHIVE_DELETE_HOT_AFTER_ARCHIVE
    };

    public static final String[] CAMERA_HEADERS = {
            "CameraType0",
            "CameraHost0",
            "CameraPath0",
            "Codec0",
            "Username0",
            "Password0"
    };

    private List<String> headers;

    public SchemaV1() {
        headers = new LinkedList<>();
        for (var mainHeader : MAIN_HEADERS)
            headers.add(mainHeader);
        for (var cameraHeader : CAMERA_HEADERS)
            headers.add(cameraHeader);
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<String> getHeaders(int numCams) {
        List<String> tempHeaders = new ArrayList<>(headers);
        for (int i = 1; i < numCams; i++) {
            tempHeaders.add(CAMERA_TYPE + i);
            tempHeaders.add(CAMERA_HOST + i);
            tempHeaders.add(CAMERA_PATH + i);
            tempHeaders.add(CODEC + i);
            tempHeaders.add(CAMERA_USERNAME + i);
            tempHeaders.add(CAMERA_PASSWORD + i);
        }
        return tempHeaders;
    }


}
