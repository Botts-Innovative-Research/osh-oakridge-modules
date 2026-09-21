# OSCAR 3.8.3 administrator and operator manual

This manual covers the complete first-use workflow including: trusting the OSCAR TLS certificate, signing in, preparing and georeferencing a site diagram, importing lanes from a `config.csv` file, using the OSCAR Viewer, opening an alarm, and creating a Lane System manually with an RPM and cameras.

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
8. [Verify the OSCAR Viewer and open an alarm](#8-verify-the-oscar-viewer-and-open-an-alarm)
9. [Data retention and storage behavior](#9-data-retention-and-storage-behavior)
10. [Validation checklist](#10-validation-checklist)
11. [Troubleshooting](#11-troubleshooting)

## 1. Before you begin

You need:

- an installed and running OSCAR 3.8.3 deployment;
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

The first 13 headers are required, case-sensitive, and must occur in this exact order:

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth
```

Then add one six-column group per camera. Camera numbers must start at 0 and remain sequential:

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
Name,UniqueID,AutoStart,Latitude,Longitude,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
Lane01,lane01,true,35.8858,-84.2121,Rapiscan,192.0.2.10,1601,,,false,false,4.82,Axis,192.0.2.20,,H264,operator,replace-me
```

One Aspect lane with two cameras:

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0,CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
Lane02,lane02,true,35.8859,-84.2119,Aspect,192.0.2.11,502,1,32,,,,Sony,192.0.2.21,,,operator,replace-me,Custom,192.0.2.22:8554,/stream1,,operator,replace-me
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

## 8. Verify the OSCAR Viewer and open an alarm

Open `https://<oscar-host>/`. The dashboard should show lane status, the event table, and the map. When a site diagram is configured, it is the top map image and the initial extent matches its uploaded bounds. OSCAR-generated lane markers remain interactive above the diagram; a pin drawn into the source screenshot is only part of the image.

![OSCAR Viewer dashboard](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/18-viewer-dashboard.png)

### 8.1 Dashboard checks

1. Confirm that every expected lane appears in **Lane Status**.
2. Confirm that the lane marker is near the expected point on the diagram.
3. Use the layer control to compare the site diagram with OSM or Esri when alignment needs verification.
4. Confirm that new occupancies reach the event table and that start/end time, gamma, neutron, and status values are plausible.
5. Select an event row to open the preview. The preview provides charts/video and a quick adjudication form. Enter the vehicle ID when known, select an adjudication code and secondary-inspection status, add notes, then review before submitting.
6. Use the expand action to open the full Event Details page.

### 8.2 Event Details

![Alarm Event Details](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/19-event-details.png)

The page can include:

- lane, occupancy, time, alarm status, speed, and other captured fields;
- gamma, threshold, neutron, and supported RS-350 charts;
- recorded video for the event window;
- miscellaneous event data and prior adjudications;
- uploaded evidence and QR-scanned evidence;
- optional Web ID analysis and isotope selection;
- vehicle ID, notes, secondary-inspection status, and adjudication code; and
- **Export as PDF** for the rendered event report.

Available data depends on detector type, camera health, retention, event age, user permissions, and Web ID connectivity.

#### Adjudication codes

| Group | Codes |
| --- | --- |
| Real Alarm | 1 Contraband Found; 2 Other |
| Innocent Alarm | 3 Medical Isotope Found; 4 NORM Found; 5 Declared Shipment of Radioactive Material |
| False Alarm | 6 Physical Inspection Negative; 7 RIID/ASP Indicates Background Only; 8 Other |
| Test/Maintenance | 9 Authorized Test, Maintenance, or Training Activity |
| Tamper/Fault | 10 Unauthorized Activity |
| Other | 11 Other |

Use site procedure—not convenience—to choose a code. The full form opens a confirmation dialog showing the vehicle ID, code/group, isotopes, notes, evidence files, scanned QR records, and secondary-inspection value before final submission.

## 9. Data retention and storage behavior

When the OSCAR Service Module has an explicit **Database ID**, it starts two database-maintenance schedules:

- every hour, it removes connection-status observations older than one hour and high-volume measurements outside buffered occupancy windows; and
- at midnight UTC, it exports the previous day's `dailyFile` output for each lane to the `dailyfiles` bucket and removes the exported database observations.

If no occupancy windows exist, non-occupancy cleanup is skipped to avoid unintended data loss. The hourly purge retains recent data with a safety margin and uses a five-second buffer around occupancy windows.

Files such as site diagrams, spreadsheets, video, reports, and daily exports are handled by the Bucket Storage Service. Database records and bucket files require separate backup planning. Video age retention and storage-pressure retention are independent: age retention decimates/deletes eligible occupancy video, while pressure retention reacts to filesystem use and currently deletes eligible video-bucket objects only.

Before changing retention, database selection, storage path, or **Delete Data on Lane Removal**, confirm the site's evidence, records, and backup requirements.

## 10. Validation checklist

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

## 11. Troubleshooting

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
| RPM does not start | Verify host reachability, exact TCP port, firewall, Aspect range, and device availability. Review the child driver status/error, not only the parent lane. |
| Camera does not start | Verify RTSP reachability and credentials, omit `rtsp://` from host, include a needed port once, confirm Axis codec or Custom path, and test the generated endpoint from the OSCAR host. |
| Video appears initially but not after refresh | Verify the camera child and HLS output remain Started and inspect server/browser logs. Refresh should not require re-creating the lane. |
| Event Details has no media | Confirm the event belongs to an available lane, required datastreams exist for its time range, retained video has not been deleted/decimated beyond need, and the user has permission. |
| Changes disappear after restart | Apply the module form, then use the administration header's global Save. |

When collecting support data, record the OSCAR version, browser, affected lane/occupancy ID, timestamp and timezone, module status/errors, and sanitized browser/server logs. Remove passwords, tokens, private keys, and sensitive evidence before sharing.

## Related documentation

- [OSCAR release Quick Start](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/QUICKSTART.md)
- [OSCAR Deployment Guide](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/DEPLOYMENT.md)
- [OSCAR translation-system guide](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/docs/TRANSLATION_SYSTEM.md)
- [Lane System module](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-system-lane)
- [OSCAR Service module](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/services/sensorhub-service-oscar)

---

Document baseline: OSCAR 3.8.3 source behavior, reviewed 2026-09-21. If a later release changes fields or workflows, update the English canonical manual and all three translations together.
