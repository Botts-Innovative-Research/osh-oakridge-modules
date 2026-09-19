# WAPIRZ-1 Motion Detector

This driver publishes motion and temperature data from a WAPIRZ-1 Z-Wave sensor.

## Configuration

- Enter a unique serial number, select the Z-Wave communication provider, and set the sensor position.
- Set the node ID and controller ID to the values assigned by the Z-Wave network.
- Enable **Reinitialize Node** only for the first run after including the device.
- Set **Retrigger Delay** to the number of minutes before motion can trigger the sensor again.
- Set the wake-up interval to at least 600 seconds.
