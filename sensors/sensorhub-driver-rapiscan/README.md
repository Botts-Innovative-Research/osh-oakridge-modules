# [NAME]

Sensor adapter for Rapiscan Driver

## Configuration

http://localhost:8282/sensorhub/admin
user: admin
pw: oscas

right click in the sensors box and add a system
- Add a UniqueId for the system
- click apply changes
- right click "New Sensor System' and in the dropdown window click 'Add Submodule'
- Click 'Rapiscan Sensor Driver'

## Configuring the Rapiscan Sensor Driver
- Under the General configuration
  - Update the following fields to coordinate to each lane
      - Gamma Count:  (Number of Gamma Detectors)
      - Neutron Count: (Number of Neutron Detectors)
      - Lane Id: (Lane Id)

- Under the Communication Provider
  - Click "Add"
  - In the pop-up window click 'TCP Comm Provider' OR 'UDP Comm Provider'
    **- TCP Comm Provider Configuration**
      - Update the following fields
          - Remote Host:
          - User/PW: (if applicable)
          - Remote Port:
    - **UDP Comm Provider Configuration**
      - Update the following fields
        - Remote Host:
        - Remote Port:
        - Local Port:

- Under the Position
  - Add location
  - Add orientation

-Click Apply Changes
- Then Right CLick 'New Sensor System' and start the driver
- After you start the System then you can add more Lanes!!!!
- 
** Repeat this for each Lane and ensure to update it to get 



# Adding A System DataBase
- Under the DataBases click the 'New System Driver DataBase'
- in the General Config
  - Add the System UIDs by clicking the green plus, the systems uids will be provided in a dropdown menu
- DataBaseConfig
  - Click Add 'H2 Historical Obs Database'
    - Add a storage path 'rapiscan.db'
  

- Start the System DataBase
- Restart the System in Sensors
