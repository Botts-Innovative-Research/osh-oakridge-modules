# Lane Sensor System

The Lane System creates a parent system for one radiation portal monitor (RPM) and one or more lane cameras.

## General configuration

- Give the lane a unique module name and unique ID.
- Enter its fixed latitude and longitude when known.
- Enable **Delete Data on Lane Removal** only when removing a lane should also delete its stored records.

## Lane options

- Under **Initial RPM Configuration**, select Aspect, Rapiscan, or RS-350 and enter the device host and port. Aspect also requires a Modbus address range.
- Under **Initial Camera Configuration**, add each Sony, Axis, or Custom camera and enter its host, username, and password when required.
- For Axis cameras, select the stream codec. For Custom cameras, enter the path after the camera host and port.
- Review all generated child modules before saving the lane.
