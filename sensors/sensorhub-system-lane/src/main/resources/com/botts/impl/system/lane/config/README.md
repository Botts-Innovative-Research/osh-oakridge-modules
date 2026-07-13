# Lane Sensor System

Specialized sensor system module used to create a lane with an RPM and video drivers.

For a mobile radiation detector (RS-350 backpack, Kromek D5 walker), use the
**Mobile Detector System** module type instead — it shares this runtime but only
exposes the options that matter for a mobile unit (see `README-mobile-detector.md`).

## Configuration

Select `Sensors` from the left-hand accordion control and right-click for the context-sensitive menu in the
accordion control.
Click 'Add New Module' and select 'Lane System' from the list of available modules.

**- General Tab:**
  - Module Name: A unique name for the Lane, must be less than 12 characters
  - UniqueID: The platform's serial number, or a unique identifier, this will be used for all submodules and must be unique.
  - Auto Start: Check the box to start this module when OSH node is launched
  - Delete Data on Lane Removal: Check the box to remove systems data from database if lane is deleted from node.

**- Fixed Location:**
  - Latitude:
  - Longitude:

**- Lane Options Config:**
- Click `Add` to configure the submodules
  - *Initial RPM Config:*
    - Click `Add` and select between the `Rapiscan`, `Aspect`, `RS350` and `D5` RPM types. To configure the RPM you need to know the host IP and port of the device. The `Aspect` RPM has an additional configuration.
      - **All RPM types**
        - Remote Host:
        - Remote Port:
      - **Aspect RPM (Specific)**
        - Address Range:
    - The RPM driver submodule is created from this config the first time the lane starts.
      **The lane-level Remote Host/Port stays authoritative afterwards**: on every lane
      (re)initialization it is pushed down into the RPM driver's communication provider,
      so this is the one place to edit the RPM's address. (A warning is logged if the
      driver's comm settings had diverged.)
    - RS350 and D5 detectors don't report occupancies natively, so the lane manages an
      occupancy process submodule for them (named `<lane> - Occupancy`). It is created
      once and reused across restarts.

  - *Initial Camera Config:*
    - Click `Add` and select between the `Axis`, `Sony` and the `Custom` video cameras. To configure the video cameras you need the host IP of the device. If applicable, you can add the username and password of the camera. Additional configurations may be necessary for different camera types.

      - **Sony/Axis/Custom**
        - Remote Host: Enter your camera's `ip.ip.ip.ip:port` (e.g., `192.168.8.77:8554`).
        - Username: Enter your camera's username in this field.
        - Password: Enter your camera's password in this field.
      - **Axis (Specific):**
        - Stream Codec:
      - **Custom (Specific):**
        - Stream Path: Enter everything that comes after your camera's `ip.ip.ip.ip:port` (e.g., `/lane04_cam`).

**- Vehicle OCR:**
  - Optional OCR of container numbers / license plates from lane camera recordings on alarm. Leave unset to disable.
