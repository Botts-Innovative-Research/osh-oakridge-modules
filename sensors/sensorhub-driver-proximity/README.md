# Proximity Switch Driver

The Proximity Switch Driver is a driver for inductive proximity switches that are connected to a Raspberry Pi.
The driver is designed to work with the Baumer inductive proximity switch IR30.P24S-N45.NV1Z.7BO,
but it may work with other inductive proximity switches as well.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Usage](#usage)
- [Wiring](#wiring)
- [Configuration](#configuration)

## Prerequisites

To use the Proximity Switch Driver, you will need:

- A Raspberry Pi.
- An inductive proximity switch, such as the Baumer IR30.P24S-N45.NV1Z.7BO.
- Java 11 or later installed on the Raspberry Pi.
- An OpenSensorHub node installed on the Raspberry Pi.

## Usage

To use the driver, follow these steps:

1. Wire the proximity switch to the Raspberry Pi as described in the [Wiring](#wiring) section.
2. Start the OpenSensorHub node and log in to the web interface.
3. Add a new driver to the node by selecting `Drivers` from the left-hand accordion control
   and right-clicking to bring up the context-sensitive menu in the accordion control.
4. Select the `Proximity Switch` driver from the list of available drivers.
5. Configure the driver as described in the [Configuration](#configuration) section.
6. Start the driver by selecting `Drivers` from the left-hand accordion control
   and right-clicking the driver in the list of drivers to bring up the context-sensitive menu.

## Wiring

The proximity switch has five pins:

- Pin 1: Brown, 6-36V DC power supply
- Pin 2: White, Normally Closed (NC)
- Pin 3: Blue, Ground
- Pin 4: Black, Normally Open (NO)
- Pin 5: Grey, not used

The proximity switch should be wired to the Raspberry Pi as follows:

- Pin 1: Connect to a 6-36V DC power supply.
  An external power supply is required to power the proximity switch.
  During testing, this pin was connected to the 5V pin on the Raspberry Pi and the proximity switch worked correctly.
- Pin 2: Connect to a GPIO pin on the Raspberry Pi.
- Pin 3: Connect to a ground pin on the Raspberry Pi.
- Pin 4: Connect to a GPIO pin on the Raspberry Pi.
- Pin 5: Leave unconnected.

The driver will work with either the NC or NO pin connected to the Raspberry Pi;
it does not require both pins to be connected, but it will use the NC pin if both are connected.

Please refer to the
[Baumer inductive proximity switch IR30.P24S-N45.NV1Z.7BO datasheet](https://www.baumer.com/us/en/product-overview/object-detection/inductive-proximity-switches/standard-cylindrical-sensors/ir30-p24s-n45-nv1z-7bo/p/25593)
for technical data, wiring diagrams, mounting instructions, and other information.

## Configuration

When added to an OpenSensorHub node, the driver has the following configuration properties:

- **General:**
    - **Module ID:** *Not editable.*
      UUID automatically assigned by OpenSensorHub for this driver instance.
    - **Module Class:** *Not editable.*
      The fully qualified name of the Java class implementing the driver.
    - **Module Name:**
      A name for the instance of the driver.
      Should be set to something short and human-readable that describes the upstream source of data.
    - **Description:** (Optional)
      A description of the driver.
    - **SensorML URL:** (Optional)
      URL to a SensorML description document for the driver or physical device the driver represents.
    - **Serial Number:**
      A string that uniquely identifies this sensor in this OpenSensorHub node.
      This is used to differentiate between multiple sensors of the same type.
    - **NC Pin:**
      GPIO pin on the Raspberry Pi connected to the Normally Closed (NC) pin of the proximity switch.
    - **NO Pin:**
      GPIO pin on the Raspberry Pi connected to the Normally Open (NO) pin of the proximity switch.

Note that only one of the NC or NO pins needs to be connected to the Raspberry Pi;
the driver will work with either or both pins connected.
