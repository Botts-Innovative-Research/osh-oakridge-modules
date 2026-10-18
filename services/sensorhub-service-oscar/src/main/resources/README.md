# OSCAR Service Module

## Administrator and operator manual

The detailed OSCAR 3.9.2 manual is maintained beside `OSCARServiceConfig`, where the administration UI can select it using the active language:

- [English](com/botts/impl/service/oscar/i18n/README.md)
- [Español](com/botts/impl/service/oscar/i18n/README_es.md)
- [Français](com/botts/impl/service/oscar/i18n/README_fr.md)
- [Ελληνικά](com/botts/impl/service/oscar/i18n/README_el.md)

Keep those four files synchronized whenever configuration or operator workflows change.

## Purpose
The purpose of this module is to handle the following aspects of OSCAR (3.0+)

### Configuration/UI
- Spreadsheet (CSV) for lanes config
- Site diagram (png/jpg)
- Site bounding box (lower-left and upper-right LLA coords)
- Video data retention parameters
  - Age at which occupancy video is processed
  - Query period
  - Delete the clip or retain a configurable number of frames
- Storage-pressure video cleanup parameters
  - Trigger and target filesystem usage
  - Check period, minimum object age, and monitored storage path

OSCAR 3.9.2 retains exactly five verified JPEG frames per eligible aging camera clip and keeps
legacy video playback for events without the new retained-media representation. High-volume
detector observations are owned by the tiered database lifecycle, while the required direct
daily-file journal publishes immutable legacy CSVs independently of database retention. Use the
release's `documentation/STORAGE_OPERATIONS.md` for archive verification, holds, portable export,
coordinated backup/restore, and tokenized deletion; do not delete cataloged files or rows manually.

### Systems/DataStreams/ControlStreams
- OSCAR client config (urn:ornl:oscar:client:config)
- Adjudication records (urn:ornl:oscar:adjudication:xxx) under each lane
- Report generation retrieval command (urn:ornl:oscar:reports)
  - POST (api/controlstreams/{id}/commands) {"startTime": "xxx", "endTime": "xxx", "reportType": "xxx"}
- DataStream/Observation for site diagram image path and bounding box

**System diagram**
- urn:ornl:oscar:node:{id}
    - "clientConfig" (list of nodes; keep current data structure)
    - "requestReport" CMD (params: startTime, endTime, reportType)
    - "siteInfo" DS
        - "siteDiagramPath" (string)
        - "siteBoundingBox" ([lat, lon], [lat, lon])

### Background Operations/Services
- Exact-five JPEG publication for eligible aging occupancy video, followed by source rollback grace
- Manifest- and hold-aware video cleanup when filesystem usage exceeds the configured threshold
- Connection-status maintenance and manifest-gated legacy daily-file observation cleanup
- Direct journal publication of immutable per-lane daily CSVs and SHA-256 manifests
- Delegation of eligible detector-observation retention to the tiered PostgreSQL/Parquet database
