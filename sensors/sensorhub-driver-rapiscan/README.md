# Rapiscan RPM Driver

Sensor adapter for Rapiscan Driver


## Overview


## Configuration

To use the Rapiscan Sensor Module, you have the option to configure three separate items in the OpenSensorHub node: a driver, TCP connection, and EML Ernie configuration.

- The driver connects to the rpm through a tcp connection and parses gamma and neutron data and publishes it to the OpenSensorHub framework.
- TCP connection allows the driver to connect to the RPM. You will need to provide and ip address, port and possibly a username and password.
- The EML service is for the VM250 RPM lanes only. Only configure this item if your lane is VM 250.

# Adding A System DataBase
General:** (*Settings common to all OpenSensorHub Systems on the "General" tab.*)
- **Module ID:** *Not editable.* UUID automatically assigned by OpenSensorHub for this driver instance.
- **Module Class:** *Not editable.* The fully qualified name of the Java class implementing the driver.
- **Module Name:** A name for the instance of the driver. Should be set to something short and human-readable that describes the upstream source of data.
- **Description:** Any descriptive text that an administrator may want to enter for the benefit of users (or themselves).
- **SensorML URL:** URL to a SensorML description document for the driver or physical device the driver represents. Typically, this is left blank, and OpenSensorHub will populate the SensorML description with sensible defaults.
- **UniqueID:** A *required* unique identifier.
- **Auto Start:** If checked, automatically start this sensor when the OSH node is launched or restarted.
- **Last Updated:** A timestamp that indicates when the settings were last changed. This can be edited if clients are watching the value for changes.

Fixed Location:
-**Latitude:**
-**Longitude:**
-**Altitude:**

Fixed Orientation:
-**Heading:**
-**Pitch:**
-**Roll:**


In the sensor system that you just created, we are going to add a rapiscan sensor that identifies as each of the lanes, and the other required sensors. 

## Configuring the Rapiscan Sensor Driver

First configure the Rapiscan Sensor. The default values are sufficient in some cases.

General:** (*Settings common to all OpenSensorHub drivers on the "General" tab.*)
- **Module ID:** *Not editable.* UUID automatically assigned by OpenSensorHub for this driver instance.
- **Module Class:** *Not editable.* The fully qualified name of the Java class implementing the driver.
- **Module Name:** A name for the instance of the driver. Should be set to something short and human-readable that describes the upstream source of data. This does not affect the operation of the driver, but is seen in user interfaces.
- **Description:** Any descriptive text that an administrator may want to enter for the benefit of users (or themselves).
- **SensorML URL:** URL to a SensorML description document for the driver or physical device the driver represents. Typically, this is left blank, and OpenSensorHub will populate the SensorML description with sensible defaults.
- **Serial Number:** A unique identifier 
- **Lane Name:** The name of the RPM lane.
- **Auto Start:** If checked, automatically start this sensor when the OSH node is launched or restarted.
- **Last Updated:** A timestamp that indicates when the settings were last changed. This can be edited if clients are watching the value for changes.
- - **Lane ID:** The lane number for the RPM system.

Communication Provider: (*Settings to add communication protocol between the driver and the rpm*)
Protocol Options
-**Remote Host:**
-**Local Address:**
-**User Name:**
-**Password:**
-**Remote Port:**
-**EnableTLS:**

Connection Options:
-**Connection Timeout:**
-**Reconnect Period:**
-**Max Reconnect Attempts:**
-**Check Reachability:**


Position Config: (*Settings for the location and orientation of the rapiscan sensor.*)
Location:
-**Latitude:**
-**Longitude:**
-**Altitude:**

Orientation:
-**Heading:**
-**Pitch:**
-**Roll:**


EML Config: (*Settings for VM250 lanes, only configure these settings if the lane requires the EML service.*)
-**Lane Width:**
-**Collimated:** If checked, 
-**Supplemental Algorithm:** If checked, this will initiate the eml service for the driver. 

-**Intervals:**
-**Occupancy Holdin:**
-**Algorithm:**
-**Sigma**
