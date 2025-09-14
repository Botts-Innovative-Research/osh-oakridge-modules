# OSCAR Service Module

## Purpose
The purpose of this module is to handle the following aspects of OSCAR (3.0+)

### Configuration/UI
- Spreadsheet (CSV) for lanes config
- Site diagram (png/jpg)
- Site bounding box (lower-left and upper-right LLA coords)
- Video data retention parameters
  - Max age for occupancy video (days)
  - 3-frame persistence (true/false)

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
- Purging/trimming of video clips past max age