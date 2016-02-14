# MAVGAnalysis

## In-Flight Analysis for PX4

This JavaFx based tool enables PX4 Users to record and analyse data published via UDP. It is not intended to replace the QGC.

**Status:** 

Last updated 14/02/2016 - <u>Important Bugfixes:</u>

- Tested on PixRacer with ESP8266
- Correct port being used now

**Features:**

- Realtime data acquisition (50ms sampling)
- Trigger recording manually or by selectable flight-mode changes
- Choosable stop-recording delay
- Display of all key-figures during and after recording
- Display of basic vehicle information, like mode, battery status, messages and sensor availability
- XY Analysis for selected key-figures
- MAVLink inspector
- OpenStreepMap viewer (requires internet access)



**How to build** (OS X, other platforms may need adjustments in `build.xml`)

- Clone repository
- Goto main directory  `cd MAVGCL-MAVGAnalysis/MAVGCL`
- Run `ant all`



**How to start:**

- Goto directory `/dist`
  
- Start UDP with `java -jar MAVGAnalysis.jar --peerAddress=172.168.178.1`
  
   *(PX4 standard ports used, replace IP with yours)*
  
  ​

**How to deploy on OSX:**

- Modify `build.xml` to adjust  `peer` property.
  
- Run `ant_deploy`
  
  ​

**Screenshots:**

![alt tag](https://raw.github.com/ecmnet/MAVGCL/MAVGAnalysis/MAVGCL/screenshot.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/MAVGAnalysis/MAVGCL/screenshot2.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/MAVGAnalysis/MAVGCL/screenshot3.png)