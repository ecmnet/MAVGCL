

# MAVGAnalysis

## In-Flight/PX4Log/ULog Analyzer for PX4 

[![Build Status](https://travis-ci.org/ecmnet/MAVGCL.svg?branch=master)](https://travis-ci.org/ecmnet/MAVGCL) [![Build status](https://ci.appveyor.com/api/projects/status/jqo0dnkcksaj6b3s?svg=true)](https://ci.appveyor.com/project/ecmnet/mavgcl) ![alt tag](https://img.shields.io/github/release/ecmnet/MAVGCL.svg)




This JavaFx based tool enables PX4 Users to record and analyse data published via UDP during flight or offline based on PX4Logs or ULogs. It is not intended to replace the QGC. It is not tested with ardupilot.

Any feedback, comments and contributions are very welcome.

**Development Status:** Last updated 07/02/2018

- Mode annotations in background for FlightMode/EKFStatus/Positioning 
- 3DView added with observer and vehicle perspective
- Java 9 compatibility

V0.6xx notes:

- V0.6 requires MAVLink 2.0 protocol without signing (switch to 'Use MAVLink 2.0 always') - MAVLink 1.0 no longer supported (for MAVLink 1.0 use V0.536 )
- Recording based on [ULOG](https://dev.px4.io/en/log/ulog_file_format.html) over MAVLink needs to be enabled in preferences (if ULog over MAVLink is not available, MAVGCL switches back to message based logging)
- Mapping between ULog data and MAVGCL key-figures is still ongoing 
- Serial connection no longer available as it is currently used for RTK base stations
- AutoPilot features only with companion running MAVSlam and MAVComm


**Features:**

- Realtime data acquisition (50ms sampling, 100ms rolling display) based on MAVLink messages or ULOG data over MAVLink
- Timechart annotated by messages (MAVLink and ULog) and parameter changes (MAVLink only)
- XY Analysis for selected key-figures
- 3D View (vehicle and observer perspective)
- MAVLink inspector (reporting raw MAVlink messages)
- Zooming (Click&Drag), pause / continue rolling update while continue recording in the background
- MAVLinkShell provides NutShell over MAVLink (Console)
- Map viewer of global position and raw gps data with option to record path (cached)
- Offline-mode: Import of key-figures from PX4Log/ULog (file or last log from device via WiFi)
- Save and load of collected data 
- Import of Custom KeyFigureMetaDataFiles allows to define *use-case specific* [collections](https://github.com/ecmnet/MAVGCL/blob/master/MAVGCL/ExampleKeyfigureMetaData.xml) of key-figures.
- Key figure conversion based on expressions
- FrSky Taranis USB supported in SITL
- Low latency MJPEG based video stream display based on [uv4l](http://www.linux-projects.org) or any other source
- Video stream recorded as H264/MP4 file while collecting data
- Virtual (calculated) key figures added (Example here: [default definition file](https://github.com/ecmnet/MAVGCL/blob/master/MAVGCL/src/com/comino/flight/model/AnalysisDataModelMetaData.xml#L1000))
- RTCM3 base supported with automatic survey-in for UBlox M8P devices (OS X only)

**Requirements:**

- requires **Java 8** JRE (Java 9 compatible)
- A companion running a serial-to-udp-proxy (either MAVComm or MAVROS, not required for PIXRacer)
- Video streaming from the vehicle requires an mjpeg streaming service (e.g.  [uv4l](http://www.linux-projects.org)) running on companion 

**Binaries:**

Available binaries can be found [here](https://github.com/ecmnet/MAVGCL/releases).

**Screenshots**:

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot5.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot9.png)

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot10.png)

**How to build on OSX** *(other platforms may need adjustments in* `build.xml`*)*:

- Clone repository
- Goto main directory  `cd MAVGCL-master/MAVGCL`
- Run `ant all`

**How to start after build  (all platforms):**

- Goto directory `/dist`

- Start with `java -jar MAVGAnalysis.jar`

- Set IP address and port in `File->Preferences` and restart (For local SITL use 127.0.0.1:14556 or start with `java -jar MAVGAnalysis.jar --SITL=true`)

- Open `demo_data.mgc`, import PX4Log file or collect data directly from your vehicle

- For video (mjpeg), setup  [uv4l](http://www.linux-projects.org) at port 8080 on your companion with :
  ​
  `uv4l --auto-video_nr --sched-rr --mem-lock --driver uvc --server-option '--port=8080'`

  Set video URL in `File->Preferences`: e.g. `http://127.0.0.1:8080/stream/video.mjpeg`

  or 

  run [MAVSlam vision]( https://github.com/ecmnet/MAVSlam) based on Intel® RealSense™ R200 on your companion and point video to port 8080 of your companion.

  ​

**How to deploy on OSX:**

- Run `ant_deploy`


**How to define custom key-figure metadata files:**

- Refer to [example file](https://github.com/ecmnet/MAVGCL/blob/master/MAVGCL/ExampleKeyfigureMetaData.xml) or [default definition file](https://github.com/ecmnet/MAVGCL/blob/master/MAVGCL/src/com/comino/flight/model/AnalysisDataModelMetaData.xml#L515)
- Conversion based on expressions ( [exp4j](http://www.objecthunter.net/exp4j/#Built-in_functions) ).
  Example: `<Converter class="ExpressionConverter" expression="1.5 * sin(val)"/>`


**How to map custom MAVLink messages to key figures**

Currently a direct mapping of MAVLink messages to keyfigures is not possible. Instead you have to generate the java class of the message and map it to the key figure:

1. Clone https://github.com/ecmnet/MAVComm and add the MAVLink definition in https://github.com/ecmnet/MAVComm/blob/master/MAVComm/mavlink/lquac.xml

   This creates the java class of the custom message and adds it to mavcomm.jar

2. Build `mavcomm.jar` with `ant build_mavcomm` and replace the one used by MAVGCL

3. Map a keyfigure definition to the created java class in the [default definition file](https://github.com/ecmnet/MAVGCL/blob/master/MAVGCL/src/com/comino/flight/model/AnalysisDataModelMetaData.xml#L515) like this


```
 <KeyFigure desc="MAVLINK Test" uom="" mask="#0.0" key="MAVLINK">
		<MAVLinkSource class="msg_altitude" field="altitude_amsl"/>
		<Validity min="0.1" max="1000.0"/>
		<Groups>
			<Group>Custom mavlink messages</Group>
		</Groups>
  </KeyFigure>
```
**Limitations:**

- Limited to one device (MAVLink-ID '1')
- Currently does not support USB or any serial connection (should be easy to add, so feel free to implement it). Note: Serial via radio might be too slow.
- Default PX4Log/ULog keyfigure mapping still not [complete](https://github.com/ecmnet/MAVGCL/blob/master/MAVGCL/src/com/comino/flight/model/AnalysisDataModelMetaData.xml), but you can add your own definition file


**Note for developers:**

MAVGAnalysis depends heavily on https://github.com/ecmnet/MAVComm for MAVLink parsing.


Please note the [License terms](https://github.com/ecmnet/MAVGCL/blob/master/MAVGCL/LICENSE.md).

