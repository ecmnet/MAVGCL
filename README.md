# MAVGAnalysis

## In-Flight/PX4Log Analysis for PX4

This JavaFx based tool enables PX4 Users to record and analyse data published via UDP during flight or based on PX4Logs. It is not intended to replace the QGC. Tested on OS X, Ubuntu and Windows.

Any comments and contributions are welcome.

**Status:** Last updated 27/04/2016 

- Import latest log from your device via WiFi and analyze it directly

**Features:**

- Realtime data acquisition (50ms sampling)
- Trigger recording manually or by selectable flight-mode/state changes
- Choosable stop-recording delay
- Display of  key-figures during and after recording (with 'Replay')
- Display of basic vehicle information (online), like mode, battery status, messages and sensor availability
- XY Analysis for selected key-figures
- MAVLink inspector
- Easy to use parameter editor
- Map viewer of global position and raw gps data with option to record path (cached)
- 3D Analysis of selected key figures (experimental)
- Import of selected key-figures from PX4Log (file or last log from device via MAVLINK)
- Save and Load of collected data 
- Low latency MJPEG based video stream display based on [uv4l](http://www.linux-projects.org/modules/sections/index.php?op=viewarticle&artid=14)  (recording and replay in preparation)

**Requirements:**

- requires **Java 8** JRE
- A companion proxy (either MAVComm or MAVROS, not required for PIXRacer)
- Video streaming requires  [uv4l](http://www.linux-projects.org/modules/sections/index.php?op=viewarticle&artid=14) running on companion 

**How to build on OSX** *(other platforms may need adjustments in* `build.xml`*)*:

- Clone repository (for stable use https://github.com/ecmnet/MAVGCL/releases)
- Goto main directory  `cd MAVGCL-master/MAVGCL`
- Run `ant all`

**How to start (all platforms):**

- Goto directory `/dist`

- Start either UDP with `java -jar MAVGAnalysis.jar --peerAddress=172.168.178.1`

   *(PX4 standard ports used, replace IP with yours)*

  or `java -jar MAVGAnalysis.jar --peerAddress=127.0.0.1` for SITL (jMAVSim)

  or just `java -jar MAVGAnalysis.jar`for a basic demo.
  ​
- Open `demo_data.mgc`, import PX4Log file or collect data directly from your vehicle
- For video (mjpeg), setup  [uv4l](http://www.linux-projects.org/modules/sections/index.php?op=viewarticle&artid=14) at port 8080 on your companion with :
  ​
  `uv4l --auto-video_nr --sched-rr --mem-lock --driver uvc --server-option '--port=8080'`

**How to deploy on OSX:**

- Modify `build.xml` to adjust  `peer` property.
- Run `ant_deploy`


**Limitations:**

- Limited to one device (MAVLink-ID '1')

- Currently does not support USB or any serial connection (should be easy to add, so feel free to implement it)

- PX4Log keyfigure mapping not complete (let me know, which missing I should add)


**Screenshots** (from PX4Log / Online acquisition):

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot1.png)



![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot2.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot3.png)

Experimental 3D Analysis (relative Local position in NED only):

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot4.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot5.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot6.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot7.png)