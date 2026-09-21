# OSCAR Service Module

## Administrator and operator manual

The detailed OSCAR 3.8.3 manual is maintained beside `OSCARServiceConfig`, where the administration UI can select it using the active language:

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
- Purging/trimming of occupancy video past the configured age
- Video cleanup when filesystem usage exceeds the configured threshold
- Hourly database cleanup outside buffered occupancy windows
- Midnight UTC export of daily-file data to CSV
