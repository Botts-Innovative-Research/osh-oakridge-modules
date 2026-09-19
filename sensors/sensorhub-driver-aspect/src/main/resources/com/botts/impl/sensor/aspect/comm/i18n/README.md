# Modbus TCP Communication Driver

This provider connects a sensor to a Modbus TCP/IP device.

## Configuration

- **Remote Host:** IP address or DNS name of the device.
- **Remote Port:** TCP port on the device; Modbus commonly uses port 502.
- **Address Range:** first and last Modbus addresses to scan.
- **Connection Options:** timeout, reconnect period, maximum attempts, and optional reachability check.
