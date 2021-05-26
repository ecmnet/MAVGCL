/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.lquac.msg_log_erase;
import org.mavlink.messages.lquac.msg_msp_command;
import org.mavlink.messages.lquac.msg_ping;

import com.comino.flight.base.UBXRTCM3Base;

import com.comino.flight.control.SITLController;
import com.comino.flight.file.FileHandler;
import com.comino.flight.log.MavlinkLogReader;
import com.comino.flight.model.map.MAVGCLMap;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.prefs.dialog.PreferencesDialog;
import com.comino.flight.ui.FlightTabs;
import com.comino.flight.ui.panel.control.FlightControlPanel;
import com.comino.flight.ui.widgets.statusline.StatusLineWidget;
import com.comino.flight.weather.MetarQNHService;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.control.impl.MAVSerialController;
import com.comino.mavcom.control.impl.MAVSimController;
import com.comino.mavcom.control.impl.MAVUdpController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavutils.legacy.ExecutorService;
import com.comino.mavutils.workqueue.WorkQueue;
import com.comino.ntp.SimpleNTPServer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader.StateChangeNotification;
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
import javafx.scene.control.DialogPane;
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
	private MenuItem m_import_last;

	@FXML
	private MenuItem m_export;

	@FXML
	private MenuItem m_reload;

	@FXML
	private MenuItem m_restart;

	@FXML
	private MenuItem m_csv;

	@FXML
	private MenuItem r_px4log;
	
	@FXML
	private MenuItem r_dellog;

	@FXML
	private MenuItem m_prefs;

	@FXML
	private MenuItem m_params;
	
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
	private MenuBar menubar;


	private static String log_filename;
	private static IMAVController control = null;
	private static FlightControlPanel controlpanel = null;

	private Stage primaryStage;
	private Scene scene;
	private BorderPane rootLayout;
	private AnchorPane flightPane;

	private UBXRTCM3Base base = null;

	private StateProperties state = null;

	private String command_line_options = null;
	
	private final WorkQueue wq = WorkQueue.getInstance();

	private SimpleNTPServer ntp_server;
	

	public MainApp() {
		super();
		
//		try {
//			redirectConsole();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
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
		//				// TODO Auto-generated method stub
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

			System.out.println("Initializing application ( Java: "+Runtime.version()+")"); 

			ExecutorService.create();
			
			
			ntp_server = new SimpleNTPServer();
			
			try {
				ntp_server.start();
			} catch (Exception e1) {
				System.out.println("NTP time server not started.");
			}

			FXMLLoadHelper.setApplication(this);

			String peerAddress = null;
			int peerport = 14555;
			int bindport = 14550;

			Map<String,String> args = getParameters().getNamed();


			Preferences userPrefs = MAVPreferences.getInstance();
			peerAddress = userPrefs.get(MAVPreferences.PREFS_IP_ADDRESS, "127.0.0.1");
			peerport = userPrefs.getInt(MAVPreferences.PREFS_IP_PORT, 14555);
			bindport = userPrefs.getInt(MAVPreferences.PREFS_BIND_PORT, 14550);
			

			if(args.size()>0) {
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
					control = new MAVUdpController("192.168.178.22",14555,14550, true);
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
				if(peerAddress.contains("127.0") || peerAddress.contains("localhost")
						||  userPrefs.getBoolean(MAVPreferences.PREFS_SITL, false)) {
					control = new MAVUdpController("127.0.0.1",14557,14540, true);
					//	new SITLController(control);
				} else {
					//	try { redirectConsole(); } catch (IOException e2) { }
					control = new MAVUdpController(peerAddress,peerport,bindport, false);
				}
			}
			
			
			MAVGCLMap.getInstance(control);

			state = StateProperties.getInstance(control);
			MAVPreferences.init();

			log_filename = control.enableFileLogging(true,userPrefs.get(MAVPreferences.PREFS_DIR,
					System.getProperty("user.home"))+"/MAVGCL");

			MSPLogger.getInstance(control);
			AnalysisModelService analysisModelService = AnalysisModelService.getInstance(control);


			if(args.get("SERIAL")==null && args.get("PROXY")==null) {
				base = UBXRTCM3Base.getInstance(control, analysisModelService);
				new Thread(base).start();
			}
			
			

			MAVGCLPX4Parameters.getInstance(control);
			

			
			state.getLPOSAvailableProperty().addListener((v,o,n) -> {
				
				// should check for homepos 
				if(!n.booleanValue() || state.getGPOSAvailableProperty().get())
					return;
				
				DataModel model = control.getCurrentModel();
				
				
				System.out.println("Detect base GPS");

				if(base !=null && base.isConnected()) {
					System.out.println("Base GPS is connected");
					if(base.getBaseAccuracy()<15) {
						msg_msp_command msp = new msg_msp_command(255,1);
						msp.command = MSP_CMD.MSP_CMD_SET_HOMEPOS;

						msp.param1  = (long)(base.getLatitude()  * 1e7);
						msp.param2  = (long)(base.getLongitude() * 1e7);

						msp.param3  = 577*1000;

						control.sendMAVLinkMessage(msp);
						System.out.println("Global Position origin set to base position");
					}
				}
				else if(model.gps.numsat > 6 && userPrefs.getDouble(MAVPreferences.REFALT, 0) < 0) {
					
					msg_msp_command msp = new msg_msp_command(255,1);
					msp.command = MSP_CMD.MSP_CMD_SET_HOMEPOS;

					msp.param1  = (long)(model.gps.latitude * 1e7);
					msp.param2  = (long)(model.gps.longitude * 1e7);
					msp.param3  = (int)(model.gps.altitude)*1000;

					control.sendMAVLinkMessage(msp);
					System.out.println("Global Position origin set to vehicle position");
				}
				else {

					msg_msp_command msp = new msg_msp_command(255,1);
					msp.command = MSP_CMD.MSP_CMD_SET_HOMEPOS;

					msp.param1  = (long)(MAVPreferences.getInstance().getDouble(MAVPreferences.REFLAT, 0) * 1e7);
					msp.param2  = (long)(MAVPreferences.getInstance().getDouble(MAVPreferences.REFLON, 0) * 1e7);
					msp.param3  = (int)(userPrefs.getDouble(MAVPreferences.REFALT, 0))*1000;

					control.sendMAVLinkMessage(msp);
					System.out.println("Global Position origin set");
				}
				
			});

			state.getInitializedProperty().addListener((v,o,n) -> {
				if(n.booleanValue()) {
					
					control.getStatusManager().start();
					
					analysisModelService.startConverter();
					
					new SITLController(control);
					System.out.println("Initializing");

					if(command_line_options!=null && command_line_options.contains(".mgc") && !control.isConnected())
						FileHandler.getInstance().fileImport(new File(command_line_options));

					MSPLogger.getInstance().enableDebugMessages(MAVPreferences.getInstance().getBoolean(MAVPreferences.DEBUG_MSG,false));

				}
			});

			state.getConnectedProperty().addListener((e,o,n) -> {

				if(n.booleanValue()) {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_REQUEST_AUTOPILOT_CAPABILITIES, 1);
				}
				Platform.runLater(() -> {
					if(r_px4log!=null)
						r_px4log.setDisable(!n.booleanValue());
				});
			});


			wq.start();
			wq.printStatus();


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
			FileHandler.getInstance(primaryStage,control);
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
		control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
		control.shutdown();
		MAVPreferences.getInstance().putDouble("stage.x", primaryStage.getX());
		MAVPreferences.getInstance().putDouble("stage.y", primaryStage.getY());
		MAVPreferences.getInstance().putDouble("stage.width", primaryStage.getWidth());
		MAVPreferences.getInstance().putDouble("stage.height", primaryStage.getHeight());
		MAVPreferences.getInstance().flush();
		ntp_server.stop();
		try { Thread.sleep(200); } catch(Exception e) { }
		System.exit(0);

	}


	public void initRootLayout() {
		try {
			// Load root layout from fxml file.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			// Show the scene containing the root layout.
			scene = new Scene(rootLayout);
			scene.setFill(Color.rgb(32,32,32));
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

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

		try {

			menubar.setUseSystemMenuBar(true);

			String name = MAVPreferences.getInstance().get(MAVPreferences.LAST_FILE,null);
			if(name!=null) {
				m_import_last.setText("Open '"+name.substring(name.lastIndexOf("/")+1,name.length())+"'");
			}

			m_import.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					AnalysisModelService.getInstance().stop();
					FileHandler.getInstance().fileImport();
					controlpanel.getChartControl().refreshCharts();

					String name = MAVPreferences.getInstance().get(MAVPreferences.LAST_FILE,null);
					if(name!=null) {
						m_import_last.setText("Open '"+name.substring(name.lastIndexOf("/")+1,name.length())+"'");
					}
				}
			});

			m_import_last.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					AnalysisModelService.getInstance().stop();
					FileHandler.getInstance().fileImportLast();
					controlpanel.getChartControl().refreshCharts();
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

			m_params.disableProperty().bind(StateProperties.getInstance().getArmedProperty());
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
			
			r_dellog.disableProperty().bind(StateProperties.getInstance().getArmedProperty());
			r_dellog.setOnAction(event -> {
				
				Alert alert = new Alert(AlertType.CONFIRMATION,
						         "Delete all local logs from device. Do you really want tod do this?",
				                 ButtonType.OK, 
				                 ButtonType.CANCEL);
				alert.getDialogPane().getStylesheets().add(
				   getClass().getResource("application.css").toExternalForm());
				alert.setTitle("Erase local log files");
				alert.getDialogPane().getScene().setFill(Color.rgb(32,32,32));
				Optional<ButtonType> result = alert.showAndWait();

				if (result.get() == ButtonType.OK) {
				   control.sendMAVLinkMessage(new msg_log_erase(1,2));
				}
				
			});

			r_px4log.disableProperty().bind(StateProperties.getInstance().getArmedProperty());
			r_px4log.setOnAction(new EventHandler<ActionEvent>() {

				String m_text = r_px4log.getText();
				MavlinkLogReader log = new MavlinkLogReader(control);

				@Override
				public void handle(ActionEvent event) {
					//				if(state.getArmedProperty().get()) {
					//					MSPLogger.getInstance().writeLocalMsg("Unarm device before accessing log.");
					//					return;
					//				}

					AnalysisModelService.getInstance().stop();

					//	r_px4log.setText("Cancel import from device...");

					log.isCollecting().addListener((observable, oldvalue, newvalue) -> {
						if(!newvalue.booleanValue()) {
							Platform.runLater(() -> {
								r_px4log.setText(m_text);
								controlpanel.getChartControl().refreshCharts();
							});
						} else {
							r_px4log.setText("Cancel import from device...");
						}
					});

					if(log.isCollecting().get())
						log.abortReadingLog();
					else
						log.requestLastLog();
				}
			});

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

			m_prefs.disableProperty().bind(StateProperties.getInstance().getArmedProperty());
			m_prefs.setOnAction(event -> {
				new PreferencesDialog(control).show();
			});

			//	m_dump.disableProperty().bind(StateProperties.getInstance().getLogLoadedProperty().not());
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


			m_restart.disableProperty().bind(StateProperties.getInstance().getArmedProperty());
			m_restart.setOnAction((event) ->{
				msg_msp_command msp = new msg_msp_command(255,1);
				msp.command = MSP_CMD.MSP_CMD_RESTART;
				control.sendMAVLinkMessage(msp);

			});

			m_about.setVisible(true);

			//		notifyPreloader(new StateChangeNotification(
			//				StateChangeNotification.Type.BEFORE_START));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void showMAVGCLApplication() {

		try {
			// Load person overview.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("ui/MAVGCL2.fxml"));
			flightPane = (AnchorPane) loader.load();
			

			// Set person overview into the center of root layout.
			rootLayout.setCenter(flightPane);
			BorderPane.setAlignment(flightPane, Pos.TOP_LEFT);

			StatusLineWidget statusline = new StatusLineWidget();
			
			
			rootLayout.setBottom(statusline);
			
	

			controlpanel = new FlightControlPanel();
			rootLayout.setLeft(controlpanel);
			controlpanel.setup(control);

			statusline.setup(control);
			
			

			FlightTabs fvController = loader.getController();
			fvController.setup(controlpanel,statusline, control);
			fvController.setPrefHeight(820);

			if(!control.isConnected())
				control.connect();

		} catch (IOException e) {
			e.printStackTrace();
		}

		notifyPreloader(new StateChangeNotification(
				StateChangeNotification.Type.BEFORE_START));
	}


	private void showAboutDialog() {
		StringBuilder version_txt = new StringBuilder();
		VBox box = new VBox(); 
		ImageView splash = new ImageView(new Image(getClass().getResource("splash082.png").toExternalForm()));
		Label version = new Label();


		if(!Status.version.isEmpty())
			version_txt.append("  PX4 Firmware Version: "+Status.version+" ("+Status.fw_build+")");
		if(!Status.build.isEmpty() && !Status.build.contains("tmp"))
			version_txt.append("  MSP build: " + Status.build);
		version_txt.append("  MAVGCL ("+getBuildInfo().getProperty("build")+")");
		version_txt.append(" runs on Java "+System.getProperty("java.version"));
		version_txt.append(" (Resolution: "+AnalysisModelService.getInstance().getCollectorInterval_ms()+"ms)");

		version.setText(version_txt.toString());

		version.setPadding(new Insets(10,0,0,0));
		Label source = new Label("  Source, license and terms of use: https://github.com/ecmnet/MAVGCL");
		Label copyright = new Label("  ecm@gmx.de");
		box.getChildren().addAll(splash, version, source, copyright);
		box.autosize();
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About MAVGAnalysis");
		alert.getDialogPane().getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		alert.getDialogPane().setPrefHeight(220); alert.getDialogPane().setPrefWidth(600);
		alert.getDialogPane().getScene().setFill(Color.rgb(32,32,32));
		alert.getDialogPane().getChildren().addAll(box);

		Platform.runLater(() -> {
			alert.showAndWait();
		});
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
