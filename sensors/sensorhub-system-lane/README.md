# Lane Sensor System 

Specialized sensor system module to be used to create a lane with an RPM and video drivers. 

## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left-hand accordion control and right-click for the context-sensitive menu in the
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
    - Click `Add` and select between the `Rapiscan` and the `Aspect` RPMs. To configure the RPMs you need to know the host IP and port of that the device. The `Aspect` RPM has an additional configuration.
      - **Rapiscan/ Aspect RPM**
        - Remote Host:
        - Remote Port:
      - **Aspect RPM (Specific)**
        - Address Range:

    - *Initial Camera Config:*
    - Click `Add` and select between the `Axis`, `Sony` and the `Custom` video cameras. To configure the video cameras you need the host IP of the device. If applicable, you can add the username and password of the camera. Additional configurations may be necessary for different camera types. 
      - **Sony/Axis/Custom**
        - Remote Host:
        - Username:
        - Password:
      - **Axis (Specific):**
        - Stream Codec:
      - **Custom (Specific):**
        - Stream Path: