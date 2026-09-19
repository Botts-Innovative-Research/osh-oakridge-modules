# Kromek D5 Radiation Sensor

This driver connects a Kromek D5 radiation sensor and publishes selected device reports.

## Configuration

- Enter a unique serial number.
- Add the communication provider used to reach the D5 data stream.
- Under **Outputs**, enable only the reports required by your deployment.
- The radiometrics and radiometric-status reports are enabled by default; optional status, dose, isotope, threshold, and device-information reports can be enabled individually.
- Enable **Auto Start** to start the driver with the OpenSensorHub node.
