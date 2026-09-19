# Rapiscan Radiation Portal Monitor

The Rapiscan driver connects to an RPM over TCP and publishes gamma, neutron, occupancy, and related data.

## Configuration

- Enter a unique serial number and lane ID.
- Under **Communication Settings**, add a TCP provider and enter the RPM host and port.
- Configure connection timeouts, retry behavior, and reachability checks.
- Enter the sensor position when it is not inherited from a lane system.
- Use **RPM Hardware Setup** for the gamma interval, occupancy hold-in, and N-sigma threshold values.
- Enable **EML Analysis** only for a VM250 EML lane, then enter its collimation state and width.
- Enable **Auto Start** to start the driver with the node.
