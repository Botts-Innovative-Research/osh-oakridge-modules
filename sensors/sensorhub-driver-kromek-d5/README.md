# Kromek D5 RIID Sensor Driver

Sensor adapter for the Kromek D5 RIID.

## Configuring the D5 RIID device via D5 Settings App
1. Plug the D5 into the computer via USB.
2. Download and install the D5 Settings App from Kromek's website, https://www.kromek.com/software/.
3. Inside the D5 Settings App, click Open Devices and select the D5 device.
4. Click Connectivity
5. Select the Wi-Fi network and enter the password
6. If you do not know the D5's IP address, disable DHCP and enter a static IP address.

## Enabling Wi-Fi on the D5 RIID device
1. Press the down arrow to see the options.
2. Press down again until Settings is highlighted, and then press Select.
3. Find WIFI and toggle it on.
4. Press Search to return to the main screen.

## Configuring OpenSensorHub to connect to the D5 RIID device
1. In the Sensors area, right-click and add a new module. Select the Kromek D5 module.
2. In the newly added module, click the Communication Provider tab.
3. Click Add and select TCP Comm Driver.
4. Enter the IP address and port of the D5 device as configured in the D5 Settings App. Note: The D5 must be on the same network as the OSH node.
5. Right-click the sensor name in the Sensors area and click Start.

Note: If no Communication Provider is specified or the connection fails, the driver will attempt to connect to the D5 device via USB.