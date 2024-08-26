# **TSTAR API DRIVER**

This driver connects to the TSTAR (Transport Security Tracking and Reporting) API server using an HTTP POST request to 
send a username and password, and receives an authorization token in response. A list of available campaigns is 
retrieved via a GET request.  

Using the authToken & a specific campaign ID, the user is able to receive data through a websocket (jetty 
websocket api connection). The messages received are sorted based on their "changeType," of which there are 7 
different types:

- UNIT (represents detailed/specific data associated with a single campaign)
- CAMPAIGN (report status of a shipping container/truck's course of action)
- EVENT (details event type and alarm status)
- UNIT_LOG
- POSITION_LOG (campaign positional info)
- MESSAGE_LOG 
- AUDIT_LOG (user actions from within the TSTAR UI)

In the messageHandler class, the Json data from these messages is converted into Java Objects using Gson and singleton 
classes (in the "responses" directory) store the information. This data is passed in a parse(responseClass) method in 
each output, and references to the object fill the datablock. 

The TSTARHelper class constructs the majority of the dataComponents and organizes nested data in the same way as the 
received Json object. 


## Running the driver 
Before starting the driver, input TSTAR username and password to receive authToken and Campaign ID
Check that the HTTP configurations are correct: 
    - specifically remote host and remote port


## For future development
At this point the driver has only been tested with the TSTAR simulator. Therefore, it has not been tested accessing 
data from more than a single campaign. 

Also, several objects in the Json contain only null data in the sim so those will need to be updated when there 
is more information. 
