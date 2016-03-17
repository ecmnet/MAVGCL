# MAVGAnalysis

## In-Flight Analysis for PX4

This JavaFx based tool enables PX4 Users to record and analyse data published via UDP during flight. It is not intended to replace the QGC.

**Status:** Last updated 17/03/2016 

**Features:**

- Realtime data acquisition (50ms sampling)
- Trigger recording manually or by selectable flight-mode/state changes
- Choosable stop-recording delay
- Display of  key-figures during and after recording (with horizontal scrolling)
- Display of basic vehicle information, like mode, battery status, messages and sensor availability
- XY Analysis for selected key-figures
- MAVLink inspector
- OpenStreepMap viewer of global position and raw gps data with option to record path (requires internet access)
- 3D Analysis of selected key figures

**How to build on OSX** *(other platforms may need adjustments in* `build.xml`*)*:

- Clone repository
- Goto main directory  `cd MAVGCL-MAVGAnalysis/MAVGCL`
- Run `ant all`

**How to start (all platforms):**

- Goto directory `/dist`

- Start either UDP with `java -jar MAVGAnalysis.jar --peerAddress=172.168.178.1`

   *(PX4 standard ports used, replace IP with yours)*

  or `java -jar MAVGAnalysis.jar --peerAddress=127.0.0.1` for SITL (jMAVSim)

  or just `java -jar MAVGAnalysis.jar`for a basic demo.
- Import `demo_data.mgc` or collect data from your vehicle
- For video (mjpeg), setup  [uv4l](http://www.linux-projects.org/modules/sections/index.php?op=viewarticle&artid=14) at port 8080 on your companion with `uv4l --auto-video_nr --sched-rr --mem-lock --driver uvc --server-option '--port=8080'`

**How to deploy on OSX:**

- Modify `build.xml` to adjust  `peer` property.

- Run `ant_deploy`

  ​

**Screenshots**:

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot1.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot2.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot3.png)

Experimental 3D Analysis (relative Local position in NED only):

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot4.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot5.png)