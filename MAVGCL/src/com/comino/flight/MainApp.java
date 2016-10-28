/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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
import org.mavlink.messages.lquac.msg_heartbeat;

import com.comino.flight.control.SITLController;
import com.comino.flight.log.FileHandler;
import com.comino.flight.log.px4log.MAVPX4LogReader;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.panel.control.FlightControlPanel;
import com.comino.flight.parameter.PX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.prefs.dialog.PreferencesDialog;
import com.comino.flight.tabs.FlightTabs;
import com.comino.flight.widgets.statusline.StatusLineWidget;
import com.comino.mav.control.IMAVController;
import com.comino.mav.control.impl.MAVSimController;
import com.comino.mav.control.impl.MAVUdpController;
import com.comino.msp.log.MSPLogger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader.StateChangeNotification;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application  {

	private static IMAVController control = null;

	private static FlightControlPanel controlpanel = null;

	private Stage primaryStage;
	private BorderPane rootLayout;

	@FXML
	private MenuItem m_close;

	@FXML
	private MenuItem m_import;

	@FXML
	private MenuItem m_export;

	@FXML
	private MenuItem m_px4log;

	@FXML
	private MenuItem r_px4log;

	@FXML
	private MenuItem m_prefs;

	@FXML
	private MenuItem m_dump;

	@FXML
	private MenuItem m_about;

	@FXML
	private MenuBar menubar;


	public MainApp() {
		super();
	}



	@Override
	public void init() throws Exception {
		super.init();
		try {

			FXMLLoadHelper.setApplication(this);

			String peerAddress = null;
			int port = 14555;


			Map<String,String> args = getParameters().getNamed();


			Preferences userPrefs = MAVPreferences.getInstance();
			peerAddress = userPrefs.get(MAVPreferences.PREFS_IP_ADDRESS, "127.0.0.1");
			port = userPrefs.getInt(MAVPreferences.PREFS_IP_PORT, 14555);

			if(peerAddress.contains("127.0") || peerAddress.contains("localhost")) {
				control = new MAVUdpController(peerAddress,port,14550, true);
				new SITLController(control);
			}
			else
				if(args.size()>0) {
					if(args.get("SITL")!=null) {
						control = new MAVUdpController("127.0.0.1",14556,14550, true);
						new SITLController(control);
					} if(args.get("PROXY")!=null) {
						control = new MAVUdpController("127.0.0.1",14656,14650, true);
						new SITLController(control);
					}
					else if(args.get("SIM")!=null)
						control = new MAVSimController();
				}
				else
					control = new MAVUdpController(peerAddress,port,14550, false);

			StateProperties.getInstance(control);

			AnalysisModelService.getInstance(control);

			MSPLogger.getInstance(control);

			PX4Parameters.getInstance(control);

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
		System.out.println("closing...");
		control.close();
		MAVPreferences.getInstance().putDouble("stage.x", primaryStage.getX());
		MAVPreferences.getInstance().putDouble("stage.y", primaryStage.getY());
		MAVPreferences.getInstance().putDouble("stage.width", primaryStage.getWidth());
		MAVPreferences.getInstance().putDouble("stage.height", primaryStage.getHeight());
		MAVPreferences.getInstance().flush();
		super.stop();
		System.exit(0);
	}


	public static void main(String[] args) {
		launch(args);
	}

	public void initRootLayout() {
		try {
			// Load root layout from fxml file.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			// Show the scene containing the root layout.
			Scene scene = new Scene(rootLayout);
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
				FileHandler.getInstance().fileImport();
  			    controlpanel.getChartControl().refreshCharts();
			}

		});

		m_px4log.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileHandler.getInstance().fileImportLog();
				controlpanel.getChartControl().refreshCharts();
			}

		});

		r_px4log.setOnAction(new EventHandler<ActionEvent>() {

			String m_text = r_px4log.getText();
			MAVPX4LogReader log = new MAVPX4LogReader(control);

			@Override
			public void handle(ActionEvent event) {
				if(StateProperties.getInstance().getArmedProperty().get()) {
					MSPLogger.getInstance().writeLocalMsg("Unarm device before accessing log.");
					return;
				}

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
					log.cancel();
				else
					log.requestLastLog();
			}
		});

		m_export.setOnAction(event -> {
			if(AnalysisModelService.getInstance().getModelList().size()>0)
				FileHandler.getInstance().fileExport();
		});

		m_prefs.setDisable(false);
		m_prefs.setOnAction(event -> {
			new PreferencesDialog(control).show();
		});

		m_dump.setOnAction(event -> {
			AnalysisDataModelMetaData.getInstance().dump();
		});

		m_about.setOnAction(event -> {
			showAboutDialog();
		});

		m_about.setVisible(true);

		notifyPreloader(new StateChangeNotification(
				StateChangeNotification.Type.BEFORE_START));


	}


	public void showMAVGCLApplication() {

		try {
			// Load person overview.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("tabs/MAVGCL2.fxml"));
			AnchorPane flightPane = (AnchorPane) loader.load();

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
	}

	private void showAboutDialog() {
		VBox box = new VBox();
		ImageView splash = new ImageView(new Image(getClass().getResource("splash.png").toExternalForm()));
		box.getChildren().addAll(splash);
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.getDialogPane().getChildren().add(box);
		alert.getDialogPane().setPrefHeight(291); alert.getDialogPane().setPrefWidth(600);
		Platform.runLater(() -> {
			alert.showAndWait();
		});

	}


}
