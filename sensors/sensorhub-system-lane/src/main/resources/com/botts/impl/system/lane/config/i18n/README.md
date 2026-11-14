# Lane Sensor System

The **Lane Sensor System** creates a parent lane container for one Radiation Portal Monitor (RPM), its occupancy data process, and up to four lane cameras.

## Plain-Language Terms

| Term | Meaning |
| --- | --- |
| **Operational View Key** | An optional tag (for example, `north-gate` or `cargo-inbound`) that groups this lane into a named operator workspace in the OSCAR Viewer. |
| **Extended Occupancy Threshold (min)** | Duration in minutes (`1`–`1440`, default `1`) before an active occupancy is logged as a `Fault - Extended Occupancy` event. Any gamma-only alarm (no neutron alarm) during an extended occupancy is automatically adjudicated as `Code 9998: Extended Occupancy Gamma Alarm`. |
| **Enhanced Machine Learning (EML)** | Optional Rapiscan algorithm that compares low-, mid-, and high-energy gamma counts to distinguish natural cargo radiation (**NORM**, auto-adjudicated as `Code 9999: EML Non-Threat`) from concentrated threat sources. |
| **Vehicle OCR (`ocrConfig`)** | Optional per-lane background OCR (`Enable Vehicle OCR = false` by default) that asynchronously reads ISO 6346 shipping container codes (with check-digit verification) and license plates from occupancy camera clips as CPU and heap resources allow, publishing `vehicleOcr` (`PARQUET_PRIMARY`) suggestions, bounded crop thumbnails (`files/ocr/crops`, `2,000`-file FIFO cap), and **Vehicle ID** auto-fill during adjudication. When `Alarms Only` is cleared, non-alarm clips are scanned transiently and deleted immediately after OCR while keeping `occupancy.videoPaths` empty (`[]`). |
| **Modbus Address Range** | Register range (`From` and `To`) used by Aspect RPM controllers over TCP/IP. |

## General Configuration

- **Module Name** and **Lane UID**: Give the lane a unique display name and unique ID (`id` suffix appended to `urn:osh:system:lane:`).
- **Operational View Key**: Optional view key used to assign this lane to an Operational View in OSCAR Service.
- **Extended Occupancy Threshold (min)**: Duration in minutes (default `1` minute) before an active occupancy logs an extended occupancy fault and enables gamma-only extended occupancy auto-adjudication.
- **Vehicle OCR**: Optional per-lane OCR settings (`Enable Vehicle OCR`, `Alarms Only`, `Model Directory`, `Read Container Numbers`, `Read License Plates`, `Camera UIDs`, `Frames Per Clip`, `Container Min Confidence`, `Plate Min Confidence`, `Ignore Top Band Fraction`, `Ignore Bottom Band Fraction`). Disabled by default with zero runtime overhead when off.
- **Location**: Enter the lane's fixed latitude and longitude (or place the lane pin on the Site Diagram in OSCAR Service).
- **Delete Data on Lane Removal**: Enable **only** when deleting this lane should also permanently purge its historical stored records.

## Lane Options

- **Initial RPM Configuration**:
  - Select **Rapiscan**, **Aspect**, or **RS-350** and enter the device IP/host and TCP port (`10001` typical for Rapiscan, `502` typical for Aspect Modbus, `1627` typical for RS-350).
  - For **Aspect**, enter the Modbus address range (`From` and `To`).
  - For **Rapiscan**, configure **EML Configuration** (`EML Enabled`, `One Panel Mode`, `Custom Parameters`, and `Use Auto Accumulate Time`) if the node has an uploaded `.jar` EML plugin.
- **Initial Camera Configuration**:
  - Add up to **4** **Sony**, **Axis**, or **Custom** cameras per lane.
  - For **Axis** cameras, select the stream codec (`H.264`). For **Custom** cameras, enter the RTSP/HTTP stream path after the host and port.
- Click **Apply** and **Save** in the top toolbar so SensorHub creates and starts the child RPM, occupancy process, and camera modules.
