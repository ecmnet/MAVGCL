/****************************************************************************
 *
 *   Copyright (c) 2017,2022 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.mavlink.generator.MAVLinkMessage;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.lquac.msg_autopilot_version;
import org.mavlink.messages.lquac.msg_log_erase;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.file.MAVFTPClient;
import com.comino.flight.events.MAVEventMataData;
import com.comino.flight.file.FileHandler;
import com.comino.flight.log.ulog.MavLinkULOGHandler;
import com.comino.flight.model.map.MAVGCLMap;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.observables.VoiceHandler;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.prefs.dialog.PreferencesDialog;
import com.comino.flight.ui.FlightTabs;
import com.comino.flight.ui.panel.control.FlightControlPanel;
import com.comino.flight.ui.widgets.statusline.StatusLineWidget;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.control.impl.MAVAutoController;
import com.comino.mavcom.control.impl.MAVSerialController;
import com.comino.mavcom.control.impl.MAVSimController;
import com.comino.mavcom.control.impl.MAVUdpController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavutils.legacy.ExecutorService;
import com.comino.mavutils.workqueue.WorkQueue;
import com.comino.ntp.SimpleNTPServer;

import boofcv.BoofVersion;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainApp extends Application  {

	@FXML
	private MenuItem m_close;

	@FXML
	private MenuItem m_import;


	@FXML
	private MenuItem m_recent1;

	@FXML
	private MenuItem m_recent2;

	@FXML
	private MenuItem m_recent3;

	@FXML
	private MenuItem m_export;

	@FXML
	private MenuItem m_reload;


	@FXML
	private MenuItem m_restart;

	@FXML
	private MenuItem m_csv;

	@FXML
	private MenuItem r_px4log_s;


	@FXML
	private MenuItem r_dellog;

	@FXML
	private MenuItem m_prefs;

	@FXML
	private MenuItem m_params;
	
	@FXML
	private MenuItem m_ftp;

	@FXML
	private MenuItem m_map;

	@FXML
	private MenuItem m_dump;

	@FXML
	private MenuItem m_pdoc;

	@FXML
	private MenuItem m_about;

	@FXML
	private MenuItem m_def;

	@FXML
	private MenuItem m_log;
	

	@FXML
	private CheckMenuItem m_video_as_background;

	@FXML
	private MenuBar menubar;


	private static String log_filename;
	private static IMAVController control = null;
	private static FlightControlPanel controlpanel = null;

	private static Stage primaryStage;
	private Scene scene;
	private BorderPane rootLayout;
	private AnchorPane flightPane;

	private StateProperties state = null;

	private String command_line_options = null;
	
	private FlightTabs fvController;

	private final WorkQueue wq = WorkQueue.getInstance();

	private AnalysisModelService analysisModelService;
	private SimpleNTPServer ntp_server;

	private long startup;

	public MainApp() {
		super();
		startup = System.currentTimeMillis();

		//		try {
		//			redirectConsole();
		//		} catch (IOException e) {
		//			
		//			e.printStackTrace();
		//		}


		//		com.sun.glass.ui.Application glassApp = com.sun.glass.ui.Application.GetApplication();
		//		glassApp.setEventHandler(new com.sun.glass.ui.Application.EventHandler() {
		//			@Override
		//			public void handleOpenFilesAction(com.sun.glass.ui.Application app, long time, String[] filenames) {
		//				super.handleOpenFilesAction(app, time, filenames);
		//				if(filenames[0]!=null) {
		//					command_line_options = filenames[0];
		//					if(FileHandler.getInstance()!=null && command_line_options.contains(".mgc") && control!=null && !control.isConnected())
		//						FileHandler.getInstance().fileImport(new File(filenames[0]));
		//
		//				}
		//			}
		//
		//			@Override
		//			public void handleQuitAction(com.sun.glass.ui.Application app, long time) {
		//				
		//				super.handleQuitAction(app, time);
		//				control.close();
		//				System.exit(0);
		//			}
		//		});

	}


	@Override
	public void init() throws Exception {
		try {

			// To avoid MJPEG warnings
			Logger.getLogger("javafx.scene.image").setLevel(Level.SEVERE);

			System.out.println("Initializing application ( Java: "+Runtime.version()+")"+" "+System.getProperty("java.vm.vendor")+
					" JavaFX runtime "+com.sun.javafx.runtime.VersionInfo.getRuntimeVersion()+
					" build on BoofCV "+BoofVersion.VERSION); 

			ExecutorService.create();

			FXMLLoadHelper.setApplication(this);

			String peerAddress = null;
			int peerport = 14555;
			int bindport = 14550;

			Map<String,String> args = getParameters().getNamed();


			Preferences userPrefs = MAVPreferences.getInstance();
			peerAddress = userPrefs.get(MAVPreferences.PREFS_IP_ADDRESS, "127.0.0.1");
			peerport = userPrefs.getInt(MAVPreferences.PREFS_IP_PORT, 14555);
			bindport = userPrefs.getInt(MAVPreferences.PREFS_BIND_PORT, 14550);

			if(args.size()>0 ) {
				if(args.get("SITL")!=null) {
					control = new MAVUdpController("127.0.0.1",14580,14540, true);
					//new SITLController(control);
				}
				else  if(args.get("PROXY")!=null) {
					control = new MAVUdpController("127.0.0.1",14656,14650, true);
					//new SITLController(control);
				}
				else  if(args.get("SERVER")!=null) {
					System.out.println("Server");
					control = new MAVUdpController("192.168.178.156",14656,14650, true);
					//new SITLController(control);
				}
				else  if(args.get("SERIAL")!=null) {
					control = new MAVSerialController();
				}

				else if(args.get("SIM")!=null)
					control = new MAVSimController();
				else if(args.get("ip")!=null)
					peerAddress = args.get("ip");
			}
			else {


				control =new MAVAutoController(peerAddress,peerport,bindport); 


				//				if(peerAddress.contains("127.0") || peerAddress.contains("localhost")
				//						||  userPrefs.getBoolean(MAVPreferences.PREFS_SITL, false)) {
				//					control = new MAVUdpController("127.0.0.1",14557,14540, true);
				//					//	new SITLController(control);
				//				} else {
				//					//	try { redirectConsole(); } catch (IOException e2) { }
				//					control = new MAVUdpController(peerAddress,peerport,bindport, false);
				//				}
			}

			System.out.println("ControL: "+(System.currentTimeMillis()-startup)+"ms");

			state = StateProperties.getInstance(control);
			MSPLogger.getInstance(control);


			wq.start();
			control.getStatusManager().start();


			state.getInitializedProperty().addListener((v,o,n) -> {
				if(n.booleanValue()) {

					System.out.println("Initializing");

					if(command_line_options!=null && command_line_options.contains(".mgc") && !control.isConnected())
						FileHandler.getInstance().fileImport(new File(command_line_options));

					MSPLogger.getInstance().enableDebugMessages(MAVPreferences.getInstance().getBoolean(MAVPreferences.DEBUG_MSG,false));

					ntp_server = new SimpleNTPServer();

					try {
						ntp_server.start();
					} catch (Exception e1) {
						System.out.println("NTP time server not started.");
					}

				}
			});

			System.out.println(com.sun.javafx.runtime.VersionInfo.getRuntimeVersion());

			MAVEventMataData.getInstance( );

			MAVPreferences.init();
			MAVGCLMap.getInstance(control);

			System.out.println("Preferences: "+(System.currentTimeMillis()-startup)+"ms");

			MAVGCLPX4Parameters.getInstance(control);

			analysisModelService = AnalysisModelService.getInstance(control);
			analysisModelService.startConverter();

			System.out.println("Model: "+(System.currentTimeMillis()-startup)+"ms");

			state.getConnectedProperty().addListener((e,o,n) -> {
				if(n.booleanValue()) {

					//control.getStatusManager().reset();
					wq.addSingleTask("LP",500, () -> {			
						System.out.println("Is simulation: "+control.isSimulation());
						//	control.getStatusManager().reset();
						control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_REQUEST_MESSAGE, msg_autopilot_version.MAVLINK_MSG_ID_AUTOPILOT_VERSION);
						if(!control.getCurrentModel().sys.isStatus(Status.MSP_INAIR) && control.getCurrentModel().sys.isStatus(Status.MSP_ACTIVE)) {
							control.sendMSPLinkCmd(MSP_CMD.MSP_TRANSFER_MICROSLAM);
							MSPLogger.getInstance().writeLocalMsg("[mgc] grid data requested",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
						}
					});
				}
			});
			
			if(!control.isConnected())
				control.connect();

			System.out.println("Connect: "+(System.currentTimeMillis()-startup)+"ms");

			log_filename = control.enableFileLogging(true,userPrefs.get(MAVPreferences.PREFS_DIR,
					System.getProperty("user.home"))+"/MAVGCL");


			state.getLPOSAvailableProperty().addListener((v,o,n) -> {

				// should check for homepos 
				if(!n.booleanValue() || state.getGPOSAvailableProperty().get() || control.getCurrentModel().sys.isSensorAvailable(Status.MSP_GPS_AVAILABILITY))
					return;



				msg_msp_command msp = new msg_msp_command(255,1);
				msp.command = MSP_CMD.MSP_CMD_SET_HOMEPOS;

				msp.param1  = (long)(MAVPreferences.getInstance().getDouble(MAVPreferences.REFLAT, 0) * 1e7);
				msp.param2  = (long)(MAVPreferences.getInstance().getDouble(MAVPreferences.REFLON, 0) * 1e7);
				msp.param3  = (int)(userPrefs.getDouble(MAVPreferences.REFALT, 0))*1000;

				control.sendMAVLinkMessage(msp);
				System.out.println("Global Position origin set");


			});




		} catch(Exception e) {
			//	System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}




	@Override
	public void start(Stage primaryStage) {

		Locale.setDefault(Locale.ENGLISH);
		try {
			this.primaryStage = primaryStage;
			this.primaryStage.setTitle("MAVGCL Analysis");
			FileHandler.getInstance(primaryStage,control).log_cleanup();
			initRootLayout();
			showMAVGCLApplication();
		} catch(Exception e) {
			System.err.println("Errors occurred");
			e.printStackTrace();
		}
	}

	@Override
	public void stop() throws Exception {
		System.out.println("[mgc] Closing...");
		analysisModelService.close();
		control.shutdown();
		MAVPreferences.getInstance().putDouble("stage.x", primaryStage.getX());
		MAVPreferences.getInstance().putDouble("stage.y", primaryStage.getY());
		MAVPreferences.getInstance().putDouble("stage.width", primaryStage.getWidth());
		MAVPreferences.getInstance().putDouble("stage.height", primaryStage.getHeight());
		MAVPreferences.getInstance().flush();
		ntp_server.stop();
		try { Thread.sleep(100); } catch(Exception e) { }
		System.exit(0);

	}


	public void initRootLayout() {
		try {
			// Load root layout from fxml file.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			System.out.println("Root: "+(System.currentTimeMillis()-startup)+"ms");

			rootLayout.setCenter(new Label("Initializing MAVGCL ("+getBuildInfo().getProperty("build")+") application components..."));

			// Show the scene containing the root layout.
			scene = new Scene(rootLayout);


			if(MAVPreferences.getInstance().get(MAVPreferences.PREFS_THEME,"").contains("Light")) {
				System.out.println("Loading light theme");
				scene.getStylesheets().add(getClass().getResource("light.css").toExternalForm());
			}
			else {
				System.out.println("Loading dark theme");
				scene.setFill(Color.rgb(32,32,32));
				scene.getStylesheets().add(getClass().getResource("dark.css").toExternalForm());
			}

			//			ScenicView.show(scene);


			Preferences userPrefs = MAVPreferences.getInstance();
			System.out.println("Preferences loaded");

			if(userPrefs.getDouble("stage.width", 100)>0 && userPrefs.getDouble("stage.height", 100) > 0 ) {
				primaryStage.setX(userPrefs.getDouble("stage.x", 100));
				primaryStage.setY(userPrefs.getDouble("stage.y", 100));
				primaryStage.setWidth(userPrefs.getDouble("stage.width", 1220));
				primaryStage.setHeight(userPrefs.getDouble("stage.height", 853));
			}

			primaryStage.setScene(scene);
			primaryStage.show();

			scene.setOnKeyPressed((event) -> {
				if(event.getCode()==KeyCode.TAB) {
					event.consume();
					if(control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM, 0, 21196 ))
						MSPLogger.getInstance().writeLocalMsg("EMERGENCY: User requested to switch off motors",
								MAV_SEVERITY.MAV_SEVERITY_EMERGENCY);
				}
				if(event.getCode()==KeyCode.W) {
					wq.printStatus();
				}
			});



		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void initialize() {
		menubar.setUseSystemMenuBar(true);
		setupMenuBar();
		

		state.getLogLoadedProperty().addListener((e,o,n) -> {
			Platform.runLater(() -> {
				setupMenuBar();
			});
		});
	}


	public void showMAVGCLApplication() {

		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainApp.class.getResource("ui/MAVGCL2.fxml"));

		Platform.runLater(() -> {

			try {
				flightPane = (AnchorPane) loader.load();

			} catch (IOException e) {
				e.printStackTrace();
			}

			rootLayout.setCenter(flightPane);
			BorderPane.setAlignment(flightPane, Pos.TOP_LEFT);

			StatusLineWidget statusline = new StatusLineWidget();
			statusline.setup(control);

			rootLayout.setBottom(statusline);

			controlpanel = new FlightControlPanel();
			rootLayout.setLeft(controlpanel);
			controlpanel.setup(control);


			fvController = loader.getController();
			fvController.setup(controlpanel,statusline, control);
			fvController.setPrefHeight(820);


		});

		wq.addSingleTask("LP", 1000, () -> {
			VoiceHandler.getInstance(control);
		});


		primaryStage.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			if(newValue.booleanValue() && control.isConnected()) {
				control.getStatusManager().reset(); 
			}
		});



		//		notifyPreloader(new StateChangeNotification(
		//				StateChangeNotification.Type.BEFORE_START));
	}

	public static Stage getPrimaryStage() {
		return primaryStage;
	}


	private void showAboutDialog() {
		StringBuilder version_txt = new StringBuilder();
		VBox box = new VBox(); 
		ImageView splash = new ImageView(new Image(getClass().getResource("splash082.png").toExternalForm()));
		Label version = new Label();


		if(!Status.version.isEmpty())
			version_txt.append("  PX4 FW Vers.: "+Status.version+" ("+Status.fw_build+")");
		if(!Status.build.isEmpty() && !Status.build.contains("tmp"))
			version_txt.append("  MSP build: " + Status.build);
		version_txt.append("  MAVGCL ("+getBuildInfo().getProperty("build")+")");
		version_txt.append(" runs on Java "+Runtime.version());
		version_txt.append("/"+com.sun.javafx.runtime.VersionInfo.getRuntimeVersion());
		version_txt.append(" (Cycle: "+AnalysisModelService.getInstance().getCollectorInterval_ms()+"ms)");

		version.setText(version_txt.toString());
		Label connect = new Label("  Connected to "+control.getConnectedAddress());
		version.setPadding(new Insets(10,0,0,0));
		Label source = new Label("  Source, license and terms of use: https://github.com/ecmnet/MAVGCL");
		Label copyright = new Label("  ecm@gmx.de");
		box.getChildren().addAll(splash, version, connect,source, copyright);
		box.autosize();
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About MAVGAnalysis");
		if(MAVPreferences.getInstance().get(MAVPreferences.PREFS_THEME,"").contains("Light")) 
			alert.getDialogPane().getStylesheets().add(getClass().getResource("light.css").toExternalForm());
		else
			alert.getDialogPane().getStylesheets().add(getClass().getResource("dark.css").toExternalForm());
		alert.getDialogPane().setPrefHeight(220); alert.getDialogPane().setPrefWidth(600);
		alert.getDialogPane().getScene().setFill(Color.rgb(32,32,32));
		alert.getDialogPane().getChildren().addAll(box);

		Platform.runLater(() -> {
			alert.showAndWait();
		});
	}

	private void setupMenuBar() {
		try {

			state = StateProperties.getInstance();

			setupLastFiles();

			m_import.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					AnalysisModelService.getInstance().stop();
					FileHandler.getInstance().fileImport();
					controlpanel.getChartControl().refreshCharts();
					setupLastFiles();
				}
			});

			m_recent1.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					AnalysisModelService.getInstance().stop();
					FileHandler.getInstance().fileImportLast(0);
					controlpanel.getChartControl().refreshCharts();
				}
			});

			m_recent2.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					AnalysisModelService.getInstance().stop();
					FileHandler.getInstance().fileImportLast(1);
					controlpanel.getChartControl().refreshCharts();
				}
			});

			m_recent3.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					AnalysisModelService.getInstance().stop();
					FileHandler.getInstance().fileImportLast(2);
					controlpanel.getChartControl().refreshCharts();
				}
			});

			m_map.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					MSPLogger.getInstance().writeLocalMsg("[mgc] grid data requested",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
					control.sendMSPLinkCmd(MSP_CMD.MSP_TRANSFER_MICROSLAM);
				}
			});

			m_log.setDisable(!System.getProperty("os.name").toUpperCase().contains("MAC"));
			m_log.setOnAction(event -> {
				try {
					Runtime.getRuntime().exec("open "+log_filename);
				} catch (IOException e) { }
			});


			m_def.setOnAction(event -> {
				FileHandler.getInstance().openKeyFigureMetaDataDefinition();
			});

			m_params.disableProperty().bind(state.getArmedProperty());
			m_params.setOnAction(event -> {
				FileHandler.getInstance().csvParameterImport();
			});


			m_reload.setOnAction((ActionEvent event)-> {
				MAVGCLPX4Parameters params = MAVGCLPX4Parameters.getInstance();
				if(params!=null)
					params.refreshParameterList(false);
			});

			m_pdoc.setOnAction(event -> {
				this.getHostServices().showDocument("https://docs.px4.io/en/advanced_config/parameter_reference.html");
			});

			r_dellog.disableProperty().bind(state.getArmedProperty().or(state.getConnectedProperty().not()));
			r_dellog.setOnAction(event -> {

				Alert alert = new Alert(AlertType.CONFIRMATION,
						"Delete all local logs from device. Do you really want tod do this?",
						ButtonType.OK, 
						ButtonType.CANCEL);
				if(MAVPreferences.isLightTheme())
					alert.getDialogPane().getStylesheets().add(
							getClass().getResource("light.css").toExternalForm());
				else
					alert.getDialogPane().getStylesheets().add(
							getClass().getResource("dark.css").toExternalForm());
				alert.setTitle("Erase local log files");
				alert.getDialogPane().getScene().setFill(Color.rgb(32,32,32));
				Optional<ButtonType> result = alert.showAndWait();

				if (result.get() == ButtonType.OK) {
					msg_log_erase msg = new msg_log_erase(1,2);
					msg.target_component = 1;
					msg.target_system    = 1;
					control.sendMAVLinkMessage(msg);
					MSPLogger.getInstance().writeLocalMsg("[mgc] All PX4 logs have been erased",
							MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				}

			});

			MavLinkULOGHandler log =  MavLinkULOGHandler.getInstance(control);

			log.isLoading().addListener((observable, oldvalue, newvalue) -> {
				if(!newvalue.booleanValue()) {

					Platform.runLater(() -> {
						r_px4log_s.setText("Import log from vehicle...");
					});
					Platform.runLater(() -> {
						controlpanel.getChartControl().refreshCharts();
					});
				} else {
					Platform.runLater(() -> {
						r_px4log_s.setText("Cancel import from vehicle");
					});
				}
			});

			r_px4log_s.disableProperty().bind(state.getArmedProperty().or(state.getConnectedProperty().not()));
			r_px4log_s.setOnAction(new EventHandler<ActionEvent>() {


				@Override
				public void handle(ActionEvent event) {

					AnalysisModelService.getInstance().stop();

					if(log.isLoading().get())
						log.cancelLoading();
					else
						log.getLog(MavLinkULOGHandler.MODE_SELECT);

				}
			});
			
			if(MAVPreferences.getInstance().get(MAVPreferences.PREFS_THEME,"").contains("Light")) {
				m_video_as_background.setDisable(true);
			}
			m_video_as_background.setOnAction((event -> {
				state.getVideoAsBackgroundProperty().set(m_video_as_background.isSelected());
			}));

			m_export.setOnAction(event -> {
				AnalysisModelService.getInstance().stop();
				if(AnalysisModelService.getInstance().getModelList().size()>0)
					FileHandler.getInstance().fileExport();
			});

			m_csv.setOnAction(event -> {
				AnalysisModelService.getInstance().stop();
				if(AnalysisModelService.getInstance().getModelList().size()>0)
					FileHandler.getInstance().csvExport();
			});

			m_prefs.disableProperty().bind(state.getArmedProperty());
			m_prefs.setOnAction(event -> {
				new PreferencesDialog(control).show();
			});

			//	m_dump.disableProperty().bind(state.getLogLoadedProperty().not());
			m_dump.setOnAction(event -> {
				FileHandler.getInstance().dumpUlogFields();
				//			try {
				//				FileHandler.getInstance().autoSave();
				//			} catch (IOException e1) {
				//
				//				e1.printStackTrace();
				//			}
			});

			m_about.setOnAction(event -> {
				showAboutDialog();
			});


			m_restart.disableProperty().bind(state.getArmedProperty().or(state.getConnectedProperty().not()));
			m_restart.setOnAction((event) ->{
				AnalysisModelService.getInstance().stop();
				msg_msp_command msp = new msg_msp_command(255,1);
				msp.command = MSP_CMD.MSP_CMD_RESTART;
				control.sendMAVLinkMessage(msp);

				AnalysisModelService.getInstance().reset();
				FileHandler.getInstance().clear();
				MAVGCLPX4Parameters.getInstance().clear();
				state.getLogLoadedProperty().set(false);

			});
			
			m_ftp.disableProperty().bind(state.getArmedProperty().or(state.getConnectedProperty().not()));
			m_ftp.setOnAction((event) ->{
				MAVFTPClient ftp = MAVFTPClient.getInstance(control);
				ftp.selectAndSendFile(MainApp.getPrimaryStage());
				ftp.close();
			});

			m_about.setVisible(true);

			//		notifyPreloader(new StateChangeNotification(
			//				StateChangeNotification.Type.BEFORE_START));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupLastFiles() {

		int li; String d;

		String s1 = MAVPreferences.getInstance().get(MAVPreferences.LAST_FILE,null);
		System.out.println(s1);
		if(s1!=null) {
			li = s1.lastIndexOf("/")+1;
			d = s1.substring(0,li-1); d = d.substring(d.lastIndexOf("/")+1, d.length());
			m_recent1.setText(s1.substring(li,s1.length())+" ["+d+"]");	
		} 

		String s2 = MAVPreferences.getInstance().get(MAVPreferences.LAST_FILE2,null);
		if(s2!=null) {
			li = s2.lastIndexOf("/")+1;
			d = s2.substring(0,li-1); d = d.substring(d.lastIndexOf("/")+1, d.length());
			m_recent2.setText(s2.substring(li,s2.length())+" ["+d+"]");	
		} else {
			m_recent2.setVisible(false);
		}

		String s3 = MAVPreferences.getInstance().get(MAVPreferences.LAST_FILE3,null);
		if(s3!=null) {
			li = s3.lastIndexOf("/")+1;
			d = s3.substring(0,li-1); d = d.substring(d.lastIndexOf("/")+1, d.length());
			m_recent3.setText(s3.substring(li,s3.length())+" ["+d+"]");	
		} else {
			m_recent3.setVisible(false);
		}

	}

	private Properties getBuildInfo() {
		Properties appProps = new Properties();
		try {
			appProps.load(getClass().getResourceAsStream("build.info"));
		} catch(IOException io ) {
		}
		return appProps;
	}

	private void redirectConsole() throws IOException {

		File file = new File("mavgcl.log");

		if(!file.exists())
			file.createNewFile();

		PrintStream fileOut = new PrintStream(file);
		System.setOut(fileOut);
		System.setErr(fileOut);

	}


}
