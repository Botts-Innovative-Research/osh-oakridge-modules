# Aspect Radiation Portal Monitor Driver

Sensor adapter for Aspect Radiation Portal Monitor.

## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left-hand accordion control and right-click for the context-sensitive menu in the
accordion control.
Click `Add New Module` and select `Aspect Sensor Driver` from the list of available drivers.

- General Tab:
  - **Module Name:** A name for the instance of the driver
  - **Serial Number:** The platform's serial number, or a unique identifier
  - **Auto Start:** Check the box to start this module when OSH node is launched

- Communication Provider Tab: Click 'Add' and select `Modbus TCP Comm Driver`
  - **Remote Host:** The host IP address of the sensor
  - **Remote Port:** The port of the sensor
  - **Address Range:** Default is from 1 to 32

  - Connection Options:
    - **Connection Timeout:** The client will wait this amount of time in ms for the remote side to respond.
    - **Reconnect Period:** Time in ms that the client will wait to reconnect when the connection is lost
    - **Max Reconnect Attempts:** The number of attempts the client will take to reconnect when connection is lost or unavailable 
    - **Check Reachability:** Enable to check if remote is reachable before taking further action

- Position Config Tab: Click 'Add' for both the 'Location' and 'Orientation' sections
  - **Location:** The location of the sensor
  - **Orientation:** The orientation of the sensor