# Aspect Radiation Portal Monitor

The Aspect driver connects an Aspect radiation portal monitor (RPM) to OpenSensorHub.

## Configuration

- Give the module a clear name and enter the sensor serial number.
- Under **Communication Settings**, add the **Modbus TCP Communication Driver**.
- Enter the RPM host, port, and Modbus address range. The usual address range is 1 through 32.
- Configure connection timeouts, retry behavior, and reachability checks as needed.
- Enter the lane ID and, when known, the sensor position and orientation.
- Enable **Auto Start** to start the driver with the OpenSensorHub node.
