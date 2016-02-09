# MAVGAnalysis

## In-Flight Analysis for PX4

This JavaFx based tool enables PX4 Users to record and analyse data published via UDP. It is not intended to replace the QGC.

**Status:** 

Last updated 08/02/2016

**Features:**

- Realtime data acquisition (50ms sampling)
- Trigger recording manually or by selectable flight-mode changes
- Choosable stop-recording delay
- Display of all key-figures during and after recording
- Display of basic vehicle information, like mode, battery status, messages and sensor availability
- XY Analysis for selected key-figures

**How to build**

- Clone repository
- Goto main directory  `cd MAVGCL-MAVGAnalysis/MAVGCL`
- Run `ant all`

**How to start:**

- Goto directory `/dist`
  
- Start with 
  
  `java -jar MAVGAnalysis.jar --peerAddress=172.168.178.1 --bindAddress=172.168.178.2`
  
   *(PX4 standard ports used, replace IPs with yours)*



**Screenshot:**

![alt tag](https://raw.github.com/ecmnet/MAVGCL/MAVGAnalysis/MAVGCL/screenshot.png)



