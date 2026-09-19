# Z-Wave Communication Service

This service manages the serial connection to a Z-Wave controller and shares discovered nodes with sensor drivers.

## Configuration

- Select the USB serial port connected to the controller.
- Set the controller's baud rate; the default is 115200.
- Enable **Master Controller** when this device is the primary Z-Wave controller.
- Enter the controller ID and its physical position.
- **Subscribed Sensor Drivers** lists the Z-Wave nodes currently associated with the service.
- Start this service before starting sensor drivers that depend on it.
