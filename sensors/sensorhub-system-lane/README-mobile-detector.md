# Mobile Detector System

Deploys a mobile radiation detector — an **RS-350 backpack** or a **Kromek D5**
walker — as a trackable system. Use this module type instead of the raw RS-350/D5
drivers (which lack the system wrapping the OSCAR viewer needs) or a Lane System
(which exposes fixed-portal options like cameras and vehicle OCR that don't apply
to a mobile unit).

The detector shows up in the OSCAR viewer like a lane: it is tracked live on the
map from its GPS output, its alarms raise Lane Status alerts and map markers, and
alarm episodes appear in the event table for adjudication.

## Configuration

Select `Sensors` in the admin UI, right-click → 'Add New Module', and choose
**Mobile Detector System**.

**- General Tab:**
  - Module Name: A unique name for the detector, must be 12 characters or less.
  - UniqueID: A unique identifier (e.g. the device serial number); used for all submodules.
  - Auto Start: Check to start this module when the OSH node launches.
  - Delete Data on Lane Removal: Check to remove the detector's data from the database when the module is deleted.

**- Detector Connection:**
  - Click `Modify` and choose the detector type: **RS-350 Backpack** or **Kromek D5**.
  - Remote Host / Remote Port: the TCP endpoint the detector's data feed is served from.

  This is the **single source of truth** for the detector's address. The detector
  driver submodule is created from it on first start, and on every (re)initialization
  the host/port is pushed back down into the driver's communication provider — so
  to repoint the detector, edit it here and restart the module. There is no need to
  touch the driver submodule's own comm settings.

## What the module manages for you

- A detector driver submodule named `<name> - RPM` (UID `urn:osh:sensor:rsi:rs350:<uniqueID>`
  or `urn:osh:sensor:kromek:d5:<uniqueID>`).
- An occupancy process submodule named `<name> - Occupancy` that converts the
  detector's alarm outputs into occupancy (alarm episode) observations for the
  event table. It is created once and reused across restarts.
- The system UID `urn:osh:system:lane:<uniqueID>` expected by the OSCAR viewer.

## Registering the curated admin form

For the trimmed-down configuration form, the Admin UI module's config must map the
config class to the form (add to the `customForms` array of the Admin UI module in
`config.json`):

```json
{
  "objClass": "org.sensorhub.ui.CustomUIConfig",
  "configClass": "com.botts.impl.system.lane.mobile.config.MobileDetectorConfig",
  "uiClass": "com.botts.ui.oscar.forms.MobileDetectorConfigForm"
}
```

Without this entry the module still works but is configured through the generic
form, which also shows the inherited fixed-lane options.
