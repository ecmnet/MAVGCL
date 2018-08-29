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

import java.io.IOException;
import java.util.Map;
import java.util.prefs.Preferences;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_gps_global_origin;

import com.comino.flight.base.UBXRTCM3Base;
import com.comino.flight.control.SITLController;
import com.comino.flight.file.FileHandler;
import com.comino.flight.log.MavlinkLogReader;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.parameter.MAVGCLPX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.prefs.dialog.PreferencesDialog;
import com.comino.flight.ui.FlightTabs;
import com.comino.flight.ui.panel.control.FlightControlPanel;
import com.comino.flight.ui.widgets.statusline.StatusLineWidget;
import com.comino.mav.control.IMAVController;
import com.comino.mav.control.impl.MAVSerialController;
import com.comino.mav.control.impl.MAVSimController;
import com.comino.mav.control.impl.MAVUdpController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.model.DataModel;

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
	private MenuItem m_export;

	@FXML
	private MenuItem r_px4log;

	@FXML
	private MenuItem m_prefs;

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


	@Override
	public void init() throws Exception {
		try {

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
					control = new MAVUdpController("127.0.0.1",14557,14540, true);
					new SITLController(control);
				}
				else  if(args.get("PROXY")!=null) {
					control = new MAVUdpController("127.0.0.1",14656,14650, true);
					new SITLController(control);
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
					new SITLController(control);
				} else
					control = new MAVUdpController(peerAddress,peerport,bindport, false);
			}

			log_filename = control.enableFileLogging(true,userPrefs.get(MAVPreferences.PREFS_DIR,
					System.getProperty("user.home"))+"/MAVGCL");


			MSPLogger.getInstance(control);
			StateProperties.getInstance(control);
			AnalysisModelService analysisModelService = AnalysisModelService.getInstance(control);
			UBXRTCM3Base.getInstance(control, analysisModelService);
			MAVGCLPX4Parameters.getInstance(control);

			StateProperties.getInstance().getConnectedProperty().addListener((v,o,n) -> {
				msg_gps_global_origin ref = new msg_gps_global_origin(255,1);
				ref.latitude  = (long)(MAVPreferences.getInstance().getDouble(MAVPreferences.REFLAT, 0) * 1e7);
				ref.longitude = (long)(MAVPreferences.getInstance().getDouble(MAVPreferences.REFLON, 0) * 1e7);
				ref.altitude  = 500*1000;
				ref.time_usec = System.currentTimeMillis() *1000;
				control.sendMAVLinkMessage(ref);
			});

		} catch(Exception e) {
			e.printStackTrace();
		}
	}



	@Override
	public void start(Stage primaryStage) {
		try {
			this.primaryStage = primaryStage;
			this.primaryStage.setTitle("MAVGCL Analysis");
			FileHandler.getInstance(primaryStage,control);
			initRootLayout();
			showMAVGCLApplication();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() throws Exception {
		MSPLogger.getInstance().writeLocalMsg("[mgc] Closing...",MAV_SEVERITY.MAV_SEVERITY_DEBUG);
		control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
		control.close();
		try { Thread.sleep(200); } catch(Exception e) { }
		MAVPreferences.getInstance().putDouble("stage.x", primaryStage.getX());
		MAVPreferences.getInstance().putDouble("stage.y", primaryStage.getY());
		MAVPreferences.getInstance().putDouble("stage.width", primaryStage.getWidth());
		MAVPreferences.getInstance().putDouble("stage.height", primaryStage.getHeight());
		MAVPreferences.getInstance().flush();
		super.stop();
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
			});

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void initialize() {

		menubar.setUseSystemMenuBar(true);

		m_import.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				AnalysisModelService.getInstance().stop();
				FileHandler.getInstance().fileImport();
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

		m_pdoc.setOnAction(event -> {
			this.getHostServices().showDocument("https://docs.px4.io/en/advanced_config/parameter_reference.html");
		});


		StateProperties.getInstance().getConnectedProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_REQUEST_AUTOPILOT_CAPABILITIES, 1);
			}
			Platform.runLater(() -> {
				r_px4log.setDisable(!n.booleanValue());
			});
		});

		r_px4log.setOnAction(new EventHandler<ActionEvent>() {

			String m_text = r_px4log.getText();
			MavlinkLogReader log = new MavlinkLogReader(control);

			@Override
			public void handle(ActionEvent event) {
				if(StateProperties.getInstance().getArmedProperty().get()) {
					MSPLogger.getInstance().writeLocalMsg("Unarm device before accessing log.");
					return;
				}

				AnalysisModelService.getInstance().stop();

				r_px4log.setText("Cancel import from device...");

				log.isCollecting().addListener((observable, oldvalue, newvalue) -> {
					if(!newvalue.booleanValue()) {
						Platform.runLater(() -> {
							r_px4log.setText(m_text);
							controlpanel.getChartControl().refreshCharts();
						});
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

		m_prefs.setDisable(false);
		m_prefs.setOnAction(event -> {
			new PreferencesDialog(control).show();
		});

		//	m_dump.disableProperty().bind(StateProperties.getInstance().getLogLoadedProperty().not());
		m_dump.setOnAction(event -> {
			FileHandler.getInstance().dumpUlogFields();
		});

		m_about.setOnAction(event -> {
			showAboutDialog();
		});

		m_about.setVisible(true);

		//		notifyPreloader(new StateChangeNotification(
		//				StateChangeNotification.Type.BEFORE_START));


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

			statusline.setup(controlpanel.getChartControl(),control);

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
		VBox box = new VBox(); DataModel model = control.getCurrentModel();
		ImageView splash = new ImageView(new Image(getClass().getResource("splash08.png").toExternalForm()));
		Label version = new Label();

		if(!model.sys.version.isEmpty())
			version_txt.append("  PX4 Firmware Version: "+model.sys.version);
		if(!model.sys.build.isEmpty() && !model.sys.build.contains("tmp"))
			version_txt.append("  MSP build: " + model.sys.build);
		version_txt.append("  MAVGCL runs on Java "+System.getProperty("java.version"));

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


}
