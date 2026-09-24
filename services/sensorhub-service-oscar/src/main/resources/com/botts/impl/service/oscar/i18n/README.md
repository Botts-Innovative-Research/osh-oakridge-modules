# OSCAR 4.0.0 administrator and operator manual

This manual covers the complete first-use and operations workflow including certificate trust, sign-in, site-diagram georeferencing, lane import/manual creation, alarm adjudication and evidence, event review, national statistics, reports, and node federation.

The screenshots show the English interface. OSCAR also supplies Spanish, French, and Greek versions of this manual, selected automatically by the administration interface:

- [Español](README_es.md)
- [Français](README_fr.md)
- [Ελληνικά](README_el.md)

> **Scope and safety.** The values visible in the screenshots are examples. Use the hostname, coordinates, device addresses, ports, usernames, passwords, retention policy, and database selected for the actual site. Configuration changes require an OSCAR administrator. Verify a certificate fingerprint with the deployment administrator before trusting it. Never import an unverified certificate or reuse the example credentials.

## Contents

1. [Before you begin](#1-before-you-begin)
2. [Trust the OSCAR certificate](#2-trust-the-oscar-certificate)
3. [Sign in and understand saving](#3-sign-in-and-understand-saving)
4. [Configure the OSCAR Service Module](#4-configure-the-oscar-service-module)
5. [Create and upload a georeferenced site diagram](#5-create-and-upload-a-georeferenced-site-diagram)
6. [Import or export lanes with `config.csv`](#6-import-or-export-lanes-with-configcsv)
7. [Create a Lane System manually](#7-create-a-lane-system-manually)
8. [Operate the OSCAR Viewer](#8-operate-the-oscar-viewer)
9. [National statistics](#9-national-statistics)
10. [Report generation](#10-report-generation)
11. [Node federation](#11-node-federation)
12. [Data retention and storage behavior](#12-data-retention-and-storage-behavior)
13. [Validation checklist](#13-validation-checklist)
14. [Troubleshooting](#14-troubleshooting)
15. [Related documentation](#15-related-documentation)

## 1. Before you begin

You need:

- an installed and running OSCAR 4.0.0 deployment;
- the OSCAR URL, normally `https://oscar.local/` unless deployment selected another hostname;
- an administrator account for `/sensorhub/admin`;
- the approved site image and the latitude/longitude of its lower-left and upper-right corners;
- the IP address or DNS name and port of every RPM;
- the IP address or DNS name, credentials, manufacturer, and stream details of every camera; and
- a supported browser on a workstation that can resolve and reach the OSCAR hostname.

Use the release [Quick Start](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/QUICKSTART.md) for installation. The [Deployment Guide](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/DEPLOYMENT.md) covers initialization, DNS, certificates, lifecycle commands, security checks, and upgrades. OSCAR has no shipped default administrator password; the password is supplied during `oscar init`.

### Coordinate and network conventions

- Latitude and longitude are decimal degrees in WGS 84. Latitude is positive north and negative south; longitude is positive east and negative west.
- A valid site rectangle must satisfy `lower-left latitude < upper-right latitude` and `lower-left longitude < upper-right longitude`. Latitude must be between -90 and 90, and longitude between -180 and 180.
- Enter a host in the form expected by the field. RPM configuration has separate host and port fields. Camera configuration has no separate port field, so include a non-default port with the camera host when necessary, for example `192.0.2.25:8554`.
- Do not add `rtsp://` to a lane camera host. The Lane System generates the RTSP URL.

## 2. Trust the OSCAR certificate

OSCAR uses HTTPS. A deployment using its generated, self-signed certificate produces a browser privacy warning until the workstation trusts that certificate. A deployment using a certificate from an already trusted organizational or public certificate authority does not require this workflow.

![Chrome certificate warning](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/01-certificate-warning.png)

### 2.1 Verify before trusting

1. Confirm that the address bar contains the exact OSCAR hostname supplied by the administrator.
2. Ask the deployment administrator for the SHA-256 fingerprint of the OSCAR certificate or issuing certificate authority.
3. Open the browser's certificate details and compare the fingerprint. Stop if it does not match.

The screenshots use Chrome on Windows. Browser wording varies by version.

### 2.2 Export the certificate from Chrome on Windows

1. Open `https://<oscar-host>/sensorhub/admin`.
2. At the privacy warning, use **Advanced** only after verifying that this is the expected OSCAR server.
3. Open the site-information control beside the address and select **Certificate details**.

![Open certificate details](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/02-certificate-details.png)

4. In the certificate viewer, select the certificate to trust, open **Details**, and choose **Export**.

![Export the certificate](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/03-certificate-export.png)

5. Save it as a Base-64-encoded X.509 certificate. Prefer a `.cer` or `.crt` extension. The `.download` extension visible in the example can still be imported if Windows recognizes it as a certificate, but `.cer` is clearer.

![Save the exported certificate](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/04-certificate-save.png)

If the deployment uses a private certificate authority, import the administrator-provided **CA certificate**, not an arbitrary server certificate downloaded from an unverified page.

### 2.3 Import it into the Windows trust store

1. Open the exported certificate and select **Install Certificate**, or run `certmgr.msc`, open **Trusted Root Certification Authorities > Certificates**, and select **Import**.
2. Choose **Current User** when trust is needed only for the signed-in user. Choose **Local Machine** only when organizational policy requires machine-wide trust and you have administrator authorization.
3. Select **Place all certificates in the following store** and choose **Trusted Root Certification Authorities**.
4. Select the exported file and finish the wizard.

![Select the certificate to import](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/05-certificate-import.png)

5. Close all browser windows, reopen the browser, and reload OSCAR. The warning should be gone and the certificate should be reported as valid for the selected hostname.

For managed workstations, distribute trust through the organization's normal certificate-management process. On macOS, import the verified certificate or CA into Keychain Access and set the approved trust policy. On Linux, use the browser or operating system trust-store process required by the distribution. The deployment guide remains authoritative for how the server certificate itself is created or replaced.

## 3. Sign in and understand saving

Open `https://<oscar-host>/sensorhub/admin` and enter the administrator credentials created during deployment.

![OSCAR administrator login](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/06-login.png)

### 3.1 Language

Use the language selector at the top of the administration interface or OSCAR Viewer. English, Spanish, French, and Greek are supported. The choice is shared between the two interfaces and takes effect when the destination interface loads or reloads. This README tab automatically opens the matching localized manual.

### 3.2 Three actions that are easy to confuse

- **Upload** immediately sends the selected file to OSCAR and, for CSV, starts importing new lanes.
- **Apply Changes** commits the currently displayed module form and updates that module's runtime configuration.
- **Save** in the administration header writes the complete module configuration so it survives a restart.

After any successful setup, use **Apply Changes** where available and then **Save**. An upload-success message proves that the file was accepted; it does not by itself prove that every device can connect or that all changes have been persisted.

## 4. Configure the OSCAR Service Module

In the left navigation, open **Services > OSCAR Service Module**.

![OSCAR Service Module general configuration](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/14-service-general.png)

### 4.1 General tab

| Field | Meaning and guidance |
| --- | --- |
| Module Class | Java implementation selected for the module. Treat it as system-managed. |
| Module Name | Human-readable name shown in the administration tree. |
| Module ID | Unique local registry ID. Keep the generated value unless performing a controlled migration. |
| Description | Optional administrator description. |
| Spreadsheet Config Path | Uploads a CSV lane configuration immediately. **Download** exports the currently loaded lanes as `config.csv`; it is unavailable when no lanes are loaded. |
| Node ID | Required unique identifier for this OSCAR node. Use the site's naming convention and do not reuse it on another node. |
| Database ID | Observation-system database used by OSCAR. If blank, statistics use the federated database, but database-specific purge/export jobs start only when a database is explicitly selected. |
| WebID API Root | Base URL of the Sandia Full Spectrum Web ID API. The default is `https://full-spectrum.sandia.gov/api/v1`. Use an approved local endpoint for an isolated deployment, or leave it blank to disable creation of the Web ID client. |
| Stats Frequency (min) | Interval, in minutes, at which statistics are published. The default is 60. |
| Auto Start | Starts the OSCAR Service Module when the node loads. This should normally be enabled in an operational deployment. |

The service requires a started Bucket Storage Service. If the module reports that it cannot find the bucket service, start and verify **Services > Bucket Storage Service**, then reinitialize or restart the OSCAR Service Module.

### 4.2 Site Diagram Config tab

This tab selects a PNG/JPG/JPEG and assigns its lower-left and upper-right geographic bounds. Section 5 gives the complete procedure.

### 4.3 Video Retention Config tab

| Field | Default | Behavior |
| --- | ---: | --- |
| Time to Keyframe Retention/Deletion (days) | 7 | Age after which an occupancy video is processed. |
| Video Query Period (minutes) | 1 | Interval between searches for eligible occupancy videos. A larger interval processes more eligible records per query. |
| Enable Frame Retention | enabled | When enabled, old clips are reduced to the configured number of retained frames. When disabled, eligible clips are deleted. |
| Keyframe Retention Count | 5 | Number of frames kept when frame retention is enabled. It has no effect when retention is disabled. |

These settings affect historical occupancy video, not the live stream configuration. Select values that satisfy site evidence-retention policy before enabling cleanup.

### 4.4 Storage Pressure Retention Config tab

| Field | Default | Behavior |
| --- | ---: | --- |
| Trigger usage (%) | 85 | Cleanup starts when the filesystem containing **Storage path** reaches this percentage. Must be greater than the target and no more than 100. |
| Target usage (%) | 80 | Cleanup continues until usage reaches this percentage, or no eligible files remain. |
| Check period (minutes) | 1 | Frequency of filesystem checks; must be greater than zero. |
| Minimum object age (minutes) | 10 | Files newer than this are protected from pressure cleanup; cannot be negative. |
| Storage path | `files` | Filesystem path whose usage controls cleanup. It must be nonempty and should identify the actual OSCAR file volume. |

Pressure cleanup removes eligible objects only from the `videos` bucket, oldest priority candidates first; daily CSV exports remain protected. Configure operating-system storage monitoring as well—this feature is an emergency pressure control, not a backup policy.

## 5. Create and upload a georeferenced site diagram

The site diagram is a north-up rectangular image mapped linearly between two geographic corners. OSCAR displays OSM by default when it is reachable and no diagram is configured. When a valid diagram is configured, the Viewer fits the map to its bounds and renders the diagram as the top raster layer; lane markers render above it.

### 5.1 Open the form

1. Open **Services > OSCAR Service Module > Site Diagram Config**.
2. If the optional configuration is absent, select **Add**.

![Empty site diagram configuration](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/07-site-diagram-empty.png)

The form contains the image upload and two coordinate pairs.

![Site diagram fields](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/08-site-diagram-fields.png)

### 5.2 Prepare the image

1. Open an approved mapping or site-plan source. If using Google Maps, follow its terms and the organization's policy for captured imagery.
2. Use a north-up, untilted view. Do not rotate or perspective-tilt the map; the OSCAR georeferencing model is an axis-aligned rectangle.
3. Frame the smallest rectangle that includes the operational area and all planned lanes. A tight rectangle gives the best default zoom.
4. Capture only the desired map rectangle and save it as `.png`, `.jpg`, or `.jpeg`.

![Source map framed for capture](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/09-source-map.png)

![Cropped site diagram](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/10-cropped-site-diagram.png)

Avoid browser chrome, menus, cursors, and large margins in the crop. Labels and a marker already present in the image are pixels, not OSCAR lane markers.

### 5.3 Obtain the bounds

Record the coordinates of the exact image corners:

- **Site Lower Left Bound**: south-west corner (`latitude`, `longitude`).
- **Site Upper Right Bound**: north-east corner (`latitude`, `longitude`).

In Google Maps, right-click a point to display and copy its decimal-degree coordinate. Repeat at the two opposing corners that match the crop.

![Copy a corner coordinate](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/11-corner-coordinate.png)

Sanity-check the values before upload:

- the upper-right latitude is larger (farther north) than the lower-left latitude;
- the upper-right longitude is larger (farther east) than the lower-left longitude—at the example US site, it is the less-negative number;
- neither pair is `0, 0`; and
- the rectangle is not so large that the facility occupies only a small portion of it.

### 5.4 Upload and persist

1. Select the image with **Choose File**.
2. Enter all four bound values before selecting **Upload**.

![Entered site diagram bounds](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/12-site-bounds-entered.png)

3. Select **Upload**. Wait for the green success message.

![Successful site diagram upload](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/13-site-upload-success.png)

4. Select **Apply Changes**, then the global **Save** button.
5. Open or reload the OSCAR Viewer and verify that the image aligns with the map and fills the initial map extent.

Uploading another valid image replaces the active diagram reference; it does not change lane coordinates. If the upload reports invalid or missing bounds, correct the coordinate ordering and range before retrying.

## 6. Import or export lanes with `config.csv`

Bulk import is the preferred way to create many lanes. For an existing site, first use **Download** to obtain an exact template generated from the loaded lanes. Treat the file as sensitive because exported camera usernames and passwords are plain text.

### 6.1 Upload workflow

1. Open **Services > OSCAR Service Module > General**.
2. At **Spreadsheet Config Path**, select a `.csv` file.

![Select a lane CSV](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/15-csv-selected.png)

3. Select **Upload** and wait for the success message.

![Successful CSV upload](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/16-csv-upload-success.png)

4. Open **Sensors** and inspect every new lane and its generated RPM/camera submodules. Loading is asynchronous, so allow time for all rows to appear.
5. Correct any per-lane values, use **Apply Changes**, and select global **Save** after validation.

![Imported lanes in Sensors](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/17-sensors-overview.png)

An uploaded row is skipped when its `UniqueID` already belongs to a loaded Lane System. Existing lanes are not overwritten. Ensure `UniqueID` is also unique within the upload file; duplicate rows in the same file are not a supported update mechanism. Parsing happens before module loading, but child-module initialization is asynchronous, so a device failure can affect one imported lane without rolling back lanes already accepted.

### 6.2 Exact schema

The current schema has 14 required, case-sensitive main headers in this exact order. `OperationalViews` is the sixth column:

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth
```

Imports remain backward-compatible with the legacy 13-header schema that omits `OperationalViews`; those lanes receive no operational-view assignments. New exports always use the 14-header schema. With the current header, every row must include the `OperationalViews` cell, although its value may be empty.

Then include at least one six-column camera group. Use an empty `CameraType0` group when the lane has no camera; additional camera numbers must remain sequential:

```csv
CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
```

There is no fixed code limit on the number of sequential camera groups, although the deployment must be sized for the total stream count. Each data row must have exactly the same number of cells as the header. The importer uses a simple comma delimiter: do not put commas or line breaks inside values, even inside quotes. Use unquoted empty cells.

### 6.3 Lane and RPM columns

| Column | Required | Accepted value and effect |
| --- | --- | --- |
| `Name` | yes | Display name. Lane initialization rejects names longer than 12 characters. |
| `UniqueID` | yes | Stable lane identifier. Do not reuse it. A plain suffix becomes an OSCAR lane URN. |
| `AutoStart` | yes | `true` or `false`. Java boolean parsing treats only case-insensitive `true` as true. |
| `Latitude` / `Longitude` | together | Decimal WGS 84 degrees. Leave both empty to omit fixed location. |
| `OperationalViews` | no | Semicolon-separated workstation view keys, for example `north-gate;secondary`. Each key must be 1–63 lowercase ASCII letters (`a`–`z`), numbers, or hyphens and cannot begin or end with a hyphen. Leave empty for no scoped-view assignment. |
| `RPMConfigType` | no | Empty for no initial RPM, or `Aspect`, `Rapiscan`, or `RS350` (case-insensitive). |
| `RPMHost` | with RPM | RPM IP address or DNS name. |
| `RPMPort` | with RPM | Integer TCP port. |
| `AspectAddressStart` / `AspectAddressEnd` | Aspect only | Inclusive Modbus address scan range; both integers. Defaults in the manual form are 1 through 32. |
| `EMLEnabled` | Rapiscan only | `true` only for a VM250/EML lane; otherwise `false`. |
| `EMLCollimated` | Rapiscan only | `true` or `false` for collimation status. |
| `LaneWidth` | Rapiscan only | Lane width in meters. Supply a valid number even when EML is disabled; the manual-form default is 4.82. |

### 6.4 Camera columns

| Column | Accepted value and effect |
| --- | --- |
| `CameraTypeN` | Empty for no camera at this index, or `Sony`, `Axis`, or `Custom` (case-insensitive). |
| `CameraHostN` | Required nonempty camera IP/DNS name. Include `:port` when a non-default RTSP port is needed. Do not include `rtsp://`. |
| `CameraPathN` | Used only by Custom. Start with `/`, for example `/stream1`. Sony and Axis generate their paths. |
| `CodecN` | Used only by Axis. `H264`/`H.264` selects H.264; `MJPEG`/`JPEG` selects Motion JPEG. |
| `UsernameN` / `PasswordN` | Camera credentials when required. Protect the CSV and delete unsecured copies after import. |

The CSV schema does not expose **Camera Video Buffer Length**; cameras imported through CSV use its default value of `0`. Adjust that value later in the camera child configuration if the site requires a buffer.

### 6.5 Minimal examples

One Rapiscan lane with one Axis camera:

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
Lane01,lane01,true,35.8858,-84.2121,north-gate,Rapiscan,192.0.2.10,1601,,,false,false,4.82,Axis,192.0.2.20,,H264,operator,replace-me
```

One Aspect lane with two cameras:

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0,CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
Lane02,lane02,true,35.8859,-84.2119,north-gate;secondary,Aspect,192.0.2.11,502,1,32,,,,Sony,192.0.2.21,,,operator,replace-me,Custom,192.0.2.22:8554,/stream1,,operator,replace-me
```

Replace all example addresses and credentials. Avoid spreadsheet software features that silently reformat identifiers, booleans, or decimal coordinates.

## 7. Create a Lane System manually

Manual creation is useful for one lane, unusual hardware, or troubleshooting. Bulk sites should normally use the CSV workflow and then review each lane.

### 7.1 Choose the correct add action

Right-click **Sensors** or use its add action to create a top-level lane. In the context menu:

- **Add New Module** creates a top-level module. Use this for a new **Lane System**.
- **Add Submodule** creates a child under the selected system. Use this to attach a driver manually to an existing lane. Do not create one Lane System inside another unless nested systems are intentionally required.
- **Restart**, **Stop**, and **Force Init** control the selected module.
- **Remove Module** removes its configuration. When **Delete Data on Lane Removal** is enabled for a lane, removal also deletes that lane's stored observations, datastreams, and system records.
- **Select/Deselect All** changes tree selection only.

![Sensor context menu](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/20-lane-context-menu.png)

Select **Lane System** in the module picker.

![Module type picker](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/21-module-picker.png)

The picker shows every installed compatible type. The pictured build includes:

| Type | Use |
| --- | --- |
| Lane System | Parent for one RPM and one or more lane cameras; use for normal OSCAR lanes. |
| Aspect Sensor Driver | Direct Aspect RPM driver, normally generated inside a lane. |
| Rapiscan Sensor Driver | Direct Rapiscan RPM driver, normally generated inside a lane. |
| RS-350 Radiation Sensor Driver | Direct RS-350 driver; a lane also creates the supporting occupancy process. |
| FFmpeg Video Driver | Direct FFmpeg-compatible camera driver, normally generated inside a lane. |
| RTSP/RTP Camera Driver | Generic RTSP/RTP camera integration; not the Lane System's manufacturer template. |
| Kromek D3S / D5 drivers | Direct Kromek detector integrations. |
| SWE Virtual Sensor | Generic sensor built from existing SWE/OSH data. |
| Sensor System | Generic parent system without OSCAR Lane System behavior. |

The available list can change with installed bundles. For ordinary lane setup, select **Lane System**, then use its initial configuration to generate supported children.

### 7.2 General tab

![New Lane System general fields](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/22-lane-general.png)

| Field | Guidance |
| --- | --- |
| Module Class | System-managed Lane System implementation. |
| Module Name | Required display name, maximum 12 characters. Use a stable site convention such as `Lane01`. |
| Module ID | Generated local registry ID. Do not copy another module's value. |
| Description | Optional operator description. |
| SensorML URL | Optional URL to a base SensorML description. Leave blank unless the site maintains one. |
| Unique ID | Required stable ID. A suffix such as `lane01` becomes `urn:osh:system:lane:lane01`; a full URN is accepted. Avoid spaces and never reuse a retired lane's ID without a migration plan. |
| Last Updated | Timestamp of the last SensorML-description update; normally left system-managed/empty unless external SensorML is used. |
| Auto Start | Starts the lane automatically when configuration loads. Enable for operational lanes after configuration is verified. |
| Operational View Keys | Optional list of workstation view keys allowed to display this lane. Add each key separately; use 1–63 lowercase ASCII letters (`a`–`z`), numbers, or hyphens, with no leading or trailing hyphen. Leave empty when the lane should have no scoped-view assignment. |
| Delete Data on Lane Removal | Defaults enabled. If selected, removing the lane deletes its database records. Clear it when decommissioning must preserve historical data. |
| Data Source Info | Optional inherited metadata shown by some builds; use only when the site's SensorML/data-source model requires it. |

### 7.3 Fixed Location tab

Enter the lane's latitude, longitude, and optional altitude (height above the WGS 84 ellipsoid, in meters).

If a valid site diagram has already been uploaded, the form displays it. Click the precise lane location and OSCAR calculates latitude and longitude from the diagram bounds. Review the calculated values before saving. You may instead type the coordinates directly.

![Select lane location on the georeferenced diagram](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/23-lane-location.png)

### 7.4 Fixed Orientation tab

Orientation uses the local North-East-Down reference frame:

- **Heading** (yaw): rotation about the Z axis, in degrees;
- **Pitch**: rotation about the Y axis, in degrees; and
- **Roll**: rotation about the X axis, in degrees.

Leave the optional orientation unset unless the site uses it. Orientation does not replace the fixed location and does not georeference the diagram.

### 7.5 Lane Options tab

Select **Add** under **Initial RPM Configuration** and the plus/**Add** control under **Initial Camera Configuration**.

![Empty initial lane options](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/24-lane-options-empty.png)

The initial options generate child modules when the Lane System initializes. They are not a second live copy of every child setting. After creation, inspect and maintain the generated RPM and camera modules under the lane in the Sensors tree.

#### RPM types

Every RPM type requires:

- **Remote Host**: device IP address or DNS name; and
- **Remote Port**: integer TCP port supplied by the device/site configuration.

Additional options:

| RPM type | Additional fields |
| --- | --- |
| Aspect | **Find Device Within Address Range — From/To**. Required inclusive Modbus scan range; defaults are 1 and 32. |
| Rapiscan | **Enable EML Analysis** only for a VM250/EML lane; **Is Collimated** records collimation status; **Lane Width (m)** defaults to 4.82. |
| RS350 | No additional initial fields. The lane creates the RS-350 driver and an occupancy process when it starts. |

Do not guess a port or Aspect address range. Confirm it with the device administrator. A failed RPM connection should not prevent independent lanes from operating, but this lane will lack normal occupancy data until its device configuration is corrected.

#### Camera types

Every camera type exposes:

- **Remote Host**: IP/DNS name and, when needed, `:port`;
- **Username** and **Password**: credentials required by the camera; and
- **Camera Video Buffer Length**: FFmpeg input-buffer setting; default `0`. Leave the default unless testing shows the site needs buffering, because a larger buffer can increase memory use and latency.

Manufacturer-specific behavior:

| Camera type | Stream behavior |
| --- | --- |
| Sony | Generates `rtsp://[credentials@]<host>:554/media/video1`. Enter the host without a scheme or duplicate port. |
| Axis | Select **H264** (default, generated 640×480 H.264 endpoint with keyframe interval 15) or **MJPEG** (generated 640×480 JPEG endpoint). |
| Custom | Enter **Stream Path**, starting with `/`. OSCAR generates `rtsp://[credentials@]<host><path>`. Include a custom port in the host. An empty path falls back to the default Axis H.264 path and should not be relied on for a custom camera. |

The generated FFmpeg children use TCP, request 24 frames per second, enable HLS output, disable individual video-frame output, use a 5-second connection timeout, and attempt reconnects. Camera capability and network capacity still determine the actual result.

![Configured RPM and camera options](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/25-lane-options-filled.png)

### 7.6 Finish and verify

1. Review every tab.
2. Add the module, or select **Apply Changes** if editing an existing lane.
3. Wait for the lane and generated children to initialize.
4. Expand the lane in **Sensors**. Confirm one intended RPM, all intended cameras, and—for RS350—the occupancy process.
5. Correct child configuration if necessary and confirm that each required module reaches **Started**.
6. Select global **Save**.

For driver-specific details, see the [Rapiscan](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-rapiscan), [Aspect](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-aspect), [RS-350](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-rs350), and [FFmpeg](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-ffmpeg) module documentation.

## 8. Operate the OSCAR Viewer

Open `https://<oscar-host>/`. The dashboard should show lane status, the event table, and the map. When a site diagram is configured, it is the top map image and the initial extent matches its uploaded bounds. OSCAR-generated lane markers remain interactive above the diagram; a pin drawn into the source screenshot is only part of the image.

![OSCAR Viewer dashboard](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/18-viewer-dashboard.png)

### 8.1 Dashboard checks and alarm queue

1. Confirm that every expected lane appears in **Lane Status**.
2. Confirm that the lane marker is near the expected point on the diagram.
3. Use the layer control to compare the site diagram with OSM or Esri when alignment needs verification.
4. Confirm that new occupancies reach the event table and that start/end time, gamma, neutron, and status values are plausible.
5. The dashboard alarm table contains alarming occupancies that have not yet been adjudicated. Select a row once to open its preview; select it again to close the preview. The selected status cell is color-coded for Gamma, Neutron, or Gamma & Neutron.
6. In the preview, review the CPS/NSIGMA chart tabs and each available recorded-camera item. Arrow controls move between media items.
7. Use the expand action to open the full Event Details page. A double-click on a table row also opens Event Details.

### 8.2 Quick adjudication on the dashboard

![Dashboard alarm preview and adjudication](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/26-dashboard-adjudication.png)

Use the dashboard form when the chart/video is sufficient and no evidence upload or WebID analysis is needed:

1. Enter **Vehicle ID** when it is known.
2. Choose one **Adjudicate** code from the grouped list in section 8.5.
3. Set **Secondary Inspection** to **None**, **Requested**, or **Completed**.
4. Add **Notes** that support the decision.
5. Select **Submit**. A success notification identifies the occupancy, and the adjudicated alarm leaves the dashboard alarm queue. Select **Reset** to clear the local form without submitting.

Submitting creates an adjudication record; it does not alter the original detector observation. Do not submit a placeholder code. If evidence, isotope selection, QR capture, WebID processing, or a review of prior decisions is required, expand to Event Details instead.

### 8.3 Events page

![Events list](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/27-event-list.png)

The **Events** page is the historical list across configured local and federated nodes. It includes all occupancies, not only active unadjudicated alarms. Each row shows the lane and parent node, occupancy ID, start/end time, maximum gamma and neutron count rates, alarm status, and whether an adjudication exists.

- **Columns** shows/hides optional columns; **Filters** opens server-side filters; **Density** changes row spacing. Filters, counts, pages, and bulk selection apply to the complete matching server-side result set, not only the currently visible page.
- Build filter groups with **All conditions (AND)** or **Any condition (OR)**, and nest groups when a workflow needs both. Up to 20 rules and three group levels are supported. Node and lane conditions choose the streams to query; occupancy ID, time, gamma/neutron maxima, alarm status, and adjudication conditions are sent to the server for each applicable lane. Applying a filter returns to page 1.
- Use the operators offered for each field. Times support before, after, and between; numeric values support inclusive limits, between, and exact match; status supports None, Gamma, Neutron, or Gamma & Neutron; adjudication supports Yes/No and empty/not empty.
- Check individual unadjudicated alarms, or select **Select all filtered alarms** to freeze the complete eligible result set at the current load time, including other pages. You may then clear individual rows. Bulk adjudication applies one code, secondary-inspection state, optional vehicle ID, and notes to the selection; it processes at most six events concurrently. It does not attach evidence or isotope selections; use Event Details when those are required.
- Review the count and warning before submitting. Successful events leave the alarm queue or show as adjudicated. Failed events remain visible and selected, and **Retry failed** retries only those failures. If OSCAR cannot enumerate the full filtered result, it selects nothing rather than silently adjudicating a partial set.
- Results are newest first and paged 15 at a time. The footer advances between pages. Counts cover the occupancy streams currently reachable from every configured node.
- Select a row for the preview. Double-click it, or choose the row's **Details** action, to open Event Details.
- If a federated node or lane is unavailable, its rows/count may be incomplete; correct node connectivity and refresh rather than assuming that zero means no events.

### 8.4 Event Details

![Event Details summary, charts, and video](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/28-event-details-summary.png)

The page can include:

- lane, occupancy, time, alarm status, speed, and other captured fields;
- gamma, threshold, neutron, and supported RS-350 charts;
- recorded video for the event window;
- WebID analysis results and the complete logged-adjudication history;
- uploaded evidence and spectroscopic QR evidence;
- optional WebID analysis and manual isotope selection;
- vehicle ID, notes, secondary-inspection status, and adjudication code; and
- **Export as PDF** for the rendered event report.

Use **Back** to return to the originating list. **CPS** and **NSIGMA** select the gamma chart presentation when both are supported. The video arrows move through camera recordings. **Export as PDF** invokes the browser print dialog for the currently rendered Event Details page; choose the browser's PDF destination to save it. This is separate from the server-generated reports in section 10.

Available data depends on detector type, camera health, retention, event age, user permissions, and WebID connectivity. A missing panel is not proof that the event had no data; verify the lane, stream, camera, and retention status.

### 8.5 Complete adjudication and evidence workflow

![Event Details evidence and adjudication](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/29-event-details-adjudication.png)

The lower half of Event Details contains three related areas:

- **WebID Analysis Results** lists timestamp, isotope name/type, confidence and confidence text, count rate, isotope text/count, warning count/text, chi square, detector response function (DRF), error message, and estimated dose. Long text has **Read more**.
- **Logged Adjudications** lists occupancy, timestamp, user, code, feedback, isotopes, evidence paths, secondary-inspection state, and vehicle ID. Evidence paths open the stored object when the node is reachable.
- **Evidence Collection** and **Adjudication Form** create a new decision. A later submission adds another logged record; it does not silently rewrite the earlier one.

#### Evidence Collection

1. Select **Upload Files** to add one or more files. OSCAR does not impose a browser file-type filter here; follow site policy and never upload unrelated or untrusted material.
2. For a spectrum that should be analyzed, enable **WebID**, select a DRF supplied by the Sandia Full Spectrum service, choose **Foreground** or **Background**, and—only for a foreground—optionally enable **Synthesize Background**. A foreground/background pair is submitted together when both are supplied.
3. Select **QR Scanner** to capture spectroscopic text with the device camera. Grant camera permission, scan one or more codes, review/delete captures, configure the same WebID/DRF/spectrum options, then select **Done**. QR captures become text evidence files when submitted.
4. Select **Upload to WebID** to process configured, not-yet-uploaded WebID evidence before the final adjudication. The button stays disabled when nothing eligible is selected. Network access to the configured bucket endpoint and Full Spectrum service is required.
5. Review returned rows. In the form, **WebID Evidence** can select one or more results; **Use Selected Result** applies their isotope findings. The operator remains responsible for the final decision.

Deleting an item before submission removes it only from the pending form. After upload, use the recorded evidence path and the site's evidence-retention procedure; do not assume browser removal deletes server evidence.

#### Adjudication Form

1. Enter **Vehicle ID** if known.
2. Select exactly one adjudication code. Select zero or more **Isotopes**; **Unknown** is mutually exclusive with named isotopes. Available named choices are Neptunium, Plutonium, Uranium-233/235/238, Americium, Barium, Bismuth, Californium, Cesium-134/137, Cobalt-57/60, Europium-152, Iridium, Manganese, Selenium, Sodium, Strontium, Fluorine, Gallium, Iodine-123/131, Indium, Palladium, Technetium, Xenon, Potassium, Radium, and Thorium.
3. Add **Notes** and select secondary inspection: **None**, **Requested**, or **Completed**.
4. Select **Submit**. Review the confirmation dialog, including code/group, vehicle, isotopes, notes, inspection state, uploaded files, and QR records. Select **Confirm and Submit** to send, or return to correct the form.
5. Confirm the success message and the new **Logged Adjudications** row. If submission fails, preserve the form, verify that the lane's adjudication control stream and node are reachable, and retry only after resolving the cause.

#### Adjudication codes

| Group | Codes |
| --- | --- |
| Real Alarm | 1 Contraband Found; 2 Other |
| Innocent Alarm | 3 Medical Isotope Found; 4 NORM Found; 5 Declared Shipment of Radioactive Material |
| False Alarm | 6 Physical Inspection Negative; 7 RIID/ASP Indicates Background Only; 8 Other |
| Test/Maintenance | 9 Authorized Test, Maintenance, or Training Activity |
| Tamper/Fault | 10 Unauthorized Activity |
| Other | 11 Other |

Use site procedure—not convenience—to choose a code.

### 8.6 Transfer an alarm by QR code in an air-gapped environment

OSCAR can package a compact alarm summary in one QR code without contacting an Internet service. The export includes source node and lane, event identity and times, status and maxima, compact adjudication metadata when present, and downsampled gamma, neutron, and threshold chart data. It does **not** include video, evidence files, spectra, or every original chart sample.

To export:

1. Open an alarm preview on the dashboard or open **Event Details** and select **Export alarm QR**.
2. Wait while OSCAR reads the event-window observations. The dialog reports exported/original gamma and neutron point counts.
3. Let the receiving device scan the displayed code, select **Download QR image** to move a printable PNG, select **Download alarm file** to save the same package as `.oscar-alarm.json`, or select **Share alarm**. If the browser cannot share files, OSCAR downloads the file instead.

The sampler keeps both endpoints, global minima/maxima, gamma points on both sides of threshold crossings, and local bucket minima/maxima before reducing the remaining points. Values are rounded to three decimal places. The package records a SHA-256 digest of the complete source series so a later system with those full data can compare them, but the omitted samples cannot be reconstructed from the QR code.

To receive:

1. Open **Alarm Transfer** from the Viewer navigation.
2. Use **Start camera scan**, **Scan QR image**, **Import alarm file**, or paste the `OSCAR-ALARM:1:` transport text. Camera scanning requires browser permission and a secure HTTPS context; image/file import remains available when camera access is prohibited.
3. OSCAR decompresses the package, enforces size and chart-data limits, verifies its SHA-256 transfer digest, and redraws the downsampled charts.
4. Confirm the node, lane, occupancy, times, status, and charts, then download or share the received alarm file if authorized.

> **Security boundary.** The package is compressed and integrity-checked, but it is neither encrypted nor digitally signed. The digest detects corruption; it does not identify the sender because a person who changes the content can compute a new digest. Treat imported data as a portable preview, independently confirm its source before operational use, and transfer it only through media and devices approved by site policy.

### 8.7 Monitor Status of Health

Open **Status of Health** with the heart-monitor icon in the Viewer navigation or go directly to `/health`. The page displays one row for every lane visible in the current scope; `/health?view=<key>` limits it to the assigned operational view.

- **Connections** reports the RPM and each configured camera as **Online**, **Offline**, or **Waiting**. Waiting means no usable current connection value has arrived and is not counted as healthy.
- **Fault status** reports gamma high, gamma low, neutron high, tamper, and extended occupancy as **Fault**, **Clear**, or **Waiting**. Missing or failed telemetry stays Waiting and is never counted as healthy or shown as clear.
- **Current occupancy** uses the Lane System's canonical `occupancyStatus` stream to show a stable elapsed time across Rapiscan, Aspect, and RS350 RPMs.
- **Last update** is the browser-local time of the latest connection or fault update for that row.

Extended occupancy defaults to one minute. Enter a value from 1 through 1440 minutes in **Extended occupancy threshold**. The setting is saved in that browser and immediately re-evaluates an occupancy already in progress; it does not alter server observations or reports.

After upgrading, confirm that each Lane System publishes `occupancyStatus` and each RPM and FFmpeg camera publishes connection status. Stop and restart an approved test camera and verify Offline then Online. Confirm unavailable fault telemetry remains Waiting, then exercise gamma, neutron, tamper, and long-occupancy tests only under the site's approved hardware procedures.

### 8.8 Use an operational-view workstation

Open `https://<oscar-host>/?view=<key>` or `https://<oscar-host>/view/<key>`. OSCAR preserves the key while navigating and limits lane discovery, events, maps, notifications, reports, alarm transfer, and Status of Health to assigned lanes. The unscoped root URL loads all lanes. Invalid keys and valid keys with no assigned lanes fail closed and load no lane data.

Operational views separate workstation presentation; they are not an authorization boundary. Use OSCAR authentication and network controls when access itself must be restricted.

## 9. National statistics

![National statistics](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/30-national-statistics.png)

The **National** page summarizes every configured node in one row per node. Columns are **Node ID**, **Gamma Alarms**, **Neutron Alarms**, **Gamma-Neutron Alarms**, **Occupancies**, **Tamper**, **Gamma Faults**, **Neutron Faults**, and total **Faults**. Use **Columns**, **Filters**, and **Density** to adjust the grid.

1. Select **All Time**, **Last 30 Days**, **Last 7 Days**, **Last 24 Hours**, or **Custom Range**.
2. For a custom range, choose both start and end date/time. The end must not precede the start.
3. Select **Refresh Statistics**. OSCAR sends a statistics-generation command to each node's OSCAR Service control stream. Custom dates are sent only for Custom Range; preset ranges use the server's corresponding stored/statistical window.
4. Wait for completion and review every row. A zero is a returned/missing numeric value, not independent proof that the node was reachable. Investigate any node-specific refresh or control-stream error.

The Viewer caches the preset ranges for quick switching and refetches them after a successful preset refresh. Results depend on each node's configured statistics schedule, retained source data, clock/timezone correctness, and network availability.

## 10. Report generation

![Report Generator with generated RDS site report](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/31-report-generator.png)

The **Report Generator** asks the selected node's OSCAR Service to create a PDF in that node's `reports` bucket, then loads the returned URL in the **Generated Report** pane.

1. Select a **Node**.
2. Select **Report Type**:
   - **RDS Site Report**: site-wide alarm totals/rates, EML suppression/rate, fault totals, and drive free/usable/total capacity.
   - **Lane Report**: the selected lane or lanes, with lane-specific alarm and fault statistics.
   - **Adjudication Report**: the selected lane or lanes, with adjudication disposition counts/percentages, isotope results, and adjudication-detail rows.
   - **Event Report**: choose **Alarms and Occupancies**, **Alarms**, or **State of Health**. These produce daily charts/tables for the selected event family; State of Health covers gamma-high, gamma-low, neutron-high, and tamper.
3. For Lane or Adjudication reports, select one or more **Lane** entries. **Select All** toggles the full node lane list; selecting it again clears the list.
4. Select **Last 24 Hours**, **Last 7 Days**, **Last 30 Days**, **This Month**, or **Custom Range**. Custom requires both date/times and the end cannot precede the start.
5. Select **Generate Report**. The request may be accepted immediately or remain pending while the server works. Leave the page open until success or a clear failure message.
6. Review the embedded PDF. Use the browser PDF controls to zoom, search, download, or print. The form resets after generation, while the generated report remains displayed.

The server retries a failed report job up to three times. Filenames encode node, report type, start, and end. Requesting the identical node/type/time combination can return the already stored report instead of overwriting it. Reports reflect retained data only; missing/expired source data cannot be reconstructed by the report generator.

## 11. Node federation

![Node federation](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/32-node-federation.png)

The **Nodes** page lets one Viewer query multiple OSCAR/OpenSensorHub nodes. The local browser-derived node is the default and cannot be deleted. Remote nodes can be added, edited, or deleted.

### 11.1 Add or edit a node

1. Enter a unique **Name** and the node **Address** (hostname or IP, without `http://` or `https://`).
2. Enter **Port**; the initial value is `8282`. Use the actual reverse-proxy or SensorHub port.
3. Enter the **Connected Systems API Endpoint**; the default is `/api`. This is appended to the fixed SensorHub root `/sensorhub`.
4. Enter **Username** and **Password** authorized to read the remote node.
5. Enable **Use a secure connection** for HTTPS/WSS/MQTTS. Use it whenever the remote endpoint supports TLS; the address/certificate name must match.
6. Authentication mode:
   - Leave **Basic-only node** clear for the preferred session-capable mode. The credentials establish an opaque HttpOnly session cookie and the password is discarded immediately after authentication.
   - Enable **Basic-only node** only when the remote node has no session-login endpoint. Credentials remain in memory for requests until this page/app is reloaded or closed.
7. Select **Add Node** or **Save**. OSCAR first tests the complete Connected Systems endpoint. An unreachable or unauthorized node is not saved. **Cancel** abandons edits.

Saved node configuration includes the name, network endpoints, TLS/authentication mode, and map metadata. Usernames and passwords are deliberately excluded from browser storage, including when older saved entries are reloaded. A session cookie is managed by the remote server; Basic-only credentials must be re-entered after reload.

Duplicate names and duplicate address/port pairs are rejected. **Edit** changes a node; **Delete** removes a remote node from this browser's federation list but does not delete the remote server or its data. A remote node must permit the Viewer's origin, credentials, API/bucket paths, WebSocket/MQTT traffic, and certificate trust as applicable.

### 11.2 Verify federation

1. Confirm the node appears in **Nodes** without an error.
2. Open Dashboard/Events and verify its lanes and occupancies show the remote node name.
3. Open National and refresh statistics; each reachable configured node should have a row.
4. Generate a node-specific report and open an event from that node to validate Connected Systems, control, bucket, and media access—not merely login.
5. After a reload, confirm session-capable nodes reconnect. Re-enter credentials for any Basic-only node.

## 12. Data retention and storage behavior

When the OSCAR Service Module has an explicit **Database ID**, it starts two database-maintenance schedules:

- every hour, it removes connection-status observations older than one hour and high-volume measurements outside buffered occupancy windows; and
- at midnight UTC, it exports the previous day's `dailyFile` output for each lane to the `dailyfiles` bucket and removes the exported database observations.

If no occupancy windows exist, non-occupancy cleanup is skipped to avoid unintended data loss. The hourly purge retains recent data with a safety margin and uses a five-second buffer around occupancy windows.

Files such as site diagrams, spreadsheets, video, reports, and daily exports are handled by the Bucket Storage Service. Database records and bucket files require separate backup planning. Video age retention and storage-pressure retention are independent: age retention decimates/deletes eligible occupancy video, while pressure retention reacts to filesystem use and currently deletes eligible video-bucket objects only.

Before changing retention, database selection, storage path, or **Delete Data on Lane Removal**, confirm the site's evidence, records, and backup requirements.

## 13. Validation checklist

### Certificate and access

- [ ] The browser opens the correct OSCAR hostname without a certificate warning.
- [ ] The trusted certificate fingerprint matches the administrator-provided fingerprint.
- [ ] Administrator sign-in works; there is no shared/default production password.
- [ ] Language selection opens the matching translated controls and help.

### OSCAR Service Module

- [ ] Node ID is unique and stable.
- [ ] Database ID, Web ID root, statistics interval, and Auto Start match site policy.
- [ ] Video and pressure-retention settings were reviewed before enabling cleanup.
- [ ] Bucket Storage Service and OSCAR Service Module are started.

### Site diagram

- [ ] Image is north-up, tightly cropped, and uses PNG/JPG/JPEG.
- [ ] Lower-left and upper-right values are valid, correctly ordered, and not zero.
- [ ] Upload reports success; Apply Changes and global Save were used.
- [ ] Viewer starts at the diagram extent; image alignment and lane-marker positions are correct.

### Lanes and devices

- [ ] Every lane name is 12 characters or fewer and every Unique ID is unique.
- [ ] RPM type, host, port, and type-specific options match the device.
- [ ] Every camera type, host/port, credentials, path/codec, and buffer value is correct.
- [ ] Expected child modules exist and reach Started; failed hardware in one lane does not block validation of other lanes.
- [ ] Global Save was selected after import/manual setup.

### Viewer

- [ ] Lane status and map markers update.
- [ ] A test occupancy appears with plausible values.
- [ ] Live and recorded camera video load, including after a browser refresh.
- [ ] Event Details opens without a client-side exception.
- [ ] A controlled test adjudication can be reviewed and submitted under site procedure.
- [ ] Nested AND/OR Events filters, all-filtered-result selection, a controlled bulk adjudication, National refresh, and each required report type were tested.
- [ ] A controlled alarm QR exports, scans/imports on the receiving device, passes integrity verification, and redraws plausible gamma/neutron curves.
- [ ] **Status of Health** lists every expected lane; RPM and cameras show Online/Offline/Waiting correctly, and approved gamma, neutron, tamper, and one-minute extended-occupancy tests produce the expected state.
- [ ] The unscoped URL shows all lanes, and each approved `?view=<key>` or `/view/<key>` URL shows only its assigned lanes across Dashboard, Events, reports, alarm transfer, and Status of Health.
- [ ] Every federated node was rechecked after a browser reload; no credential was found in browser storage.

## 14. Troubleshooting

| Symptom | Checks and corrective action |
| --- | --- |
| Browser still warns after import | Confirm hostname/SAN match, certificate validity dates, correct trust store and user/machine scope, then fully restart the browser. Never bypass a fingerprint mismatch. |
| Site upload says bounds are invalid | Add both bound objects; enter all four finite coordinates; confirm latitude/longitude ranges and that lower-left values are less than upper-right values. |
| Upload succeeds but diagram is absent | Select Apply Changes and Save, ensure OSCAR Service and Bucket Storage are Started, reload Viewer, and verify the uploaded object still exists. |
| Diagram appears too small | Recapture a tighter rectangle and enter the exact image-corner bounds. The default map extent follows those bounds. |
| Lane marker is absent | Confirm the lane has a Fixed Location, latitude/longitude were saved, and the location lies inside the diagram bounds. A marker visible only inside the source image is not an OSCAR marker. |
| OSM tiles show 403/blocked | Do not point production traffic directly at volunteer OSM tiles in violation of tile policy. Use the deployment's approved OSM provider/proxy or select Esri while the tile-service configuration is corrected. |
| CSV rejected | Compare the exact header/order, camera groups of six with sequential indexes, cell count on every row, numeric fields, and plain unquoted empty cells. Remove commas from values. |
| CSV says success but a lane is missing | Check for an existing loaded lane with the same UniqueID, a name longer than 12 characters, device-child initialization errors, and whether asynchronous loading has finished. |
| Scoped operational view is empty or rejects its key | Confirm the lane's **Operational View Keys** contains the exact lowercase key, apply and globally save the configuration, restart the lane if required, and use `?view=<key>` or `/view/<key>`. A blank assignment never matches a scoped view. |
| RPM does not start | Verify host reachability, exact TCP port, firewall, Aspect range, and device availability. Review the child driver status/error, not only the parent lane. |
| Camera does not start | Verify RTSP reachability and credentials, omit `rtsp://` from host, include a needed port once, confirm Axis codec or Custom path, and test the generated endpoint from the OSCAR host. |
| Video appears initially but not after refresh | Verify the camera child and HLS output remain Started and inspect server/browser logs. Refresh should not require re-creating the lane. |
| Event Details has no media | Confirm the event belongs to an available lane, required datastreams exist for its time range, retained video has not been deleted/decimated beyond need, and the user has permission. |
| Status of Health lane/device is missing, Waiting, or Offline | Confirm the lane is in the current view, the lane and device child are running, and the RPM or FFmpeg camera publishes connection status. Waiting means no usable status has arrived; test reachability and inspect child logs before treating it as healthy. |
| Extended occupancy does not appear | Confirm the Lane System publishes `occupancyStatus`, verify the RPM's daily-file/status input reports entry and exit, set a 1–1440 minute threshold in this browser, and wait until the stable elapsed time exceeds it. |
| Bulk adjudication partly fails | Keep the failed rows selected, review lane/node connectivity and the adjudication control stream, then use **Retry failed**. Successful rows are not resubmitted. |
| Adjudication fails | Confirm a code was selected, the lane/node is reachable, the adjudication control stream exists, and any evidence upload completed. Do not repeatedly submit until the original result is known. |
| WebID controls have no DRF/results | Verify internet access to the configured Sandia Full Spectrum service, select a DRF and foreground/background type, and inspect the returned warning/error columns. Manual adjudication remains the operator's responsibility. |
| National row is zero/missing | Refresh the intended range, verify that node's OSCAR statistics control stream and retained data, and correct node authentication/connectivity. |
| Report does not generate | Select node/type/range and required lane/event type, verify start precedes end, then check the node report control stream and `reports` bucket. An identical request may reuse an existing file. |
| Federated node disappears or rejects requests after reload | Session nodes need a valid remote cookie; Basic-only secrets are intentionally memory-only and must be re-entered. Verify TLS, CORS, paths, port, and remote authorization. |
| Alarm QR cannot be generated | Confirm the event's lane is available and its gamma or neutron datastream has retained observations for the event interval. Very large optional metadata may exceed one-code capacity; use the downloadable alarm file when QR transfer is not possible. |
| Alarm QR will not scan/import | Increase display brightness or use the downloaded PNG at original size. Otherwise import the `.oscar-alarm.json` file. An integrity failure means the package is incomplete or changed; obtain a new export rather than bypassing validation. |
| Changes disappear after restart | Apply the module form, then use the administration header's global Save. |

When collecting support data, record the OSCAR version, browser, affected lane/occupancy ID, timestamp and timezone, module status/errors, and sanitized browser/server logs. Remove passwords, tokens, private keys, and sensitive evidence before sharing.

## 15. Related documentation

- [OSCAR release Quick Start](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/QUICKSTART.md)
- [OSCAR Deployment Guide](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/DEPLOYMENT.md)
- [OSCAR translation-system guide](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/docs/TRANSLATION_SYSTEM.md)
- [Lane System module](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-system-lane)
- [OSCAR Service module](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/services/sensorhub-service-oscar)

---

Document baseline: OSCAR 4.0.0 source behavior, reviewed 2026-09-24. If a later release changes fields or workflows, update the English canonical manual and all three translations together.
