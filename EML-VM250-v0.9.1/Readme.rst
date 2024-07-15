=========
EML-VM250
=========

Version 4 ReadMe
----------------

This is the ERNIE version 4 test package.   

Requirements
~~~~~~~~~~~~

EML-VM250 utility scripts require the JPype Java/Python bridge


Software Requirements

- Java 1.11 or later
- Python3.7 or later
- ant

Python Package Requirements

- JPype 1.2 or later
- numpy
- matplotlib
- pandas
- pyzmq
- protobuf 

Windows OS Environment Variables Requirements

- JAVA_HOME = Point to the Java installation directory
- ANT_HOME  = Point to the ant installation directory

Windows OS Addition Path Requirements

- %JAVA_HOME%\bin 
- %ANT_HOME%\bin

Setting up Python
~~~~~~~~~~~~~~~~~
python3 -m pip install pandas matplotlib jpype1 pyzmq protobuf 


Building
~~~~~~~~
This package includes pre-built jar files.
After extracting, ensure the 'env.sh' file points to the correct version of Java (1.11 or later),
and `source env.sh` to setup the environment.

ERNIE decision models
~~~~~~~~~~~~~~~~~~~~~
Two decision models are included in the 'config' directory: uber_SSL_model_collimated.txt and uber_SSL_model_uncollimated.txt.
The 'uber' models were generated using all ports available to LLNL / CMU.  These models do not perform as well as site-specific
models (also available upon request), but outperform the current N-Sigma based alarm algorithm at all sites tested by LLNL
in terms of nuisance alarm reduction.

Sample Use
~~~~~~~~~~
cd EML-VM250
source env.sh
python3 py/vm250/analysisFromDailyFile.py /path/to/daily/files/\*.txt --classifier config/uber_SSL_model_collimated.txt -o collimated_results

This analyzes each daily file in the given path using the specified machine learning model.
Output files are written to the 'collimated_results' directory, one output file per daily file.


EML-VM250 Raven 
~~~~~~~~~~~~~~~~~~~~~

Additional Software Requirements:
~~~~~~~~~~~~~~~~~~~~~

- Apache NetBeans IDE for editing the Java project
- NetBeans cames with the ant software, which is located in [NETBEANS_INSTALL_DIR]/extide/ant 
- Stand-alone ant if not going to use the version 
- Visual Studio Community for editing the C# demo project and building it.

Additional Libraries Requirement:
~~~~~~~~~~~~~~~~~~~~~
    
- C#: See readme file in src/gov.llnl.ernie.raven/cs

The location of the C# project is src/gov.llnl.ernie.raven/cs/EMLVM250Demo

The location of the Python demo scripts is: src/gov.llnl.ernie.raven/py

Running the Raven Server
~~~~~~~~~~~~~~~~~~~~~~~~
#. Start a command prompt in the directory  py/raven
#. At the prompt type the command: python3 startRavenServer.py

Running C# Demo GUI
~~~~~~~~~~~~~~~~~~~
#. Follow the readme instruction in src/gov.llnl.ernie.raven/cs
#. Build and run the EMLVM250Demo project 

Running Python Test scripts
~~~~~~~~~~~~~~~~~~~~~~~~~~~
#. At root of src/gov.llnl.ernie.raven open a command prompt.
#. In the prompt run the command: ant jar 
#. Open a command prompt in src/gov.llnl.ernie.raven/py
#. Run the command: python3 testLog.py [LOG_PORT_NUMBER_FROM_THE_RAVEN_SERVER]
#. Open a command prompt in src/gov.llnl.ernie.raven/py
#. Run the command: python3 testControl.py [CONTROL_PORT_NUMBER_FROM_THE_RAVEN_SERVER]
#. Open a command prompt in src/gov.llnl.ernie.raven/py
#. Run the command: python3 testLane.py [LANE_PORT_NUMBER_FROM_THE_RAVEN_SERVER]
