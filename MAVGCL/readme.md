This is a JavaFx-based tool to provide in-flight-analysis capabilities to the PX4-Flightstack. The
intention is NOT to replace GCL.

How to run:

1. clone repository
2. change to directory 'dist'
3. start with java -jar MAVGAnalysis.jar

Limitations:

1. the current distro is bound to the local ip 172.168.178.2
2. UDP only (requires a kind of MAVProxy or direct UDP)

Known issues:

1. Connecting to UDP not working all the time
2. Limited keyfigures

Screenshot:

![alt tag](https://raw.github.com/ecmnet/MAVGCL/MAVGAnalysis/screenshot.png)
