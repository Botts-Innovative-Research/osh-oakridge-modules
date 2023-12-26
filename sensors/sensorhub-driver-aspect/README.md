# Aspect Radiation Portal Monitor Driver

Sensor adapter for Aspect Radiation Portal Monitor.

## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left-hand accordion control and right-click for the context-sensitive menu in the
accordion control.
Click 'Add New Module' and select 'Aspect Sensor Driver' from the list of available drivers.

- General Tab:
  - **Module Name:** A name for the instance of the driver
  - **Serial Number:** The platform's serial number, or a unique identifier
  - **Auto Start:** Check the box to start this module when OSH node is launched

- Communication Provider Tab: Click 'Add' and select 'Modbus TCP Comm Driver'
  - **Remote Host:** The IP address of the sensor
  - **Remote Port:** The port of the sensor

- Position Config Tab: Click 'Add' for both the 'Location' and 'Orientation' sections
  - **Location:** The location of the sensor
  - **Orientation:** The orientation of the sensor