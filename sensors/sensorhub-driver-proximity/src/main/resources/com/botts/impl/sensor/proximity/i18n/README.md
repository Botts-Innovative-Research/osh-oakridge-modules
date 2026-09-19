# Inductive Proximity Switch

This Raspberry Pi driver monitors an inductive proximity switch through GPIO inputs.

## Wiring and configuration

- Enter a unique serial number for the switch.
- Set **Normally Closed Pin** to the GPIO connected to the white NC contact when it is wired.
- Set **Normally Open Pin** to the GPIO connected to the black NO contact when it is wired.
- Select **Not Set** for a contact that is not connected. Either contact, or both contacts, may be used.
- Verify GPIO numbering and electrical compatibility before starting the driver.
- Enable **Auto Start** to monitor the switch when the node starts.
