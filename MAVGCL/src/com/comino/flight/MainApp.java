/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.flight;

import java.io.IOException;
import java.util.Map;

import com.comino.flight.control.FlightModeProperties;
import com.comino.flight.control.integration.AnalysisIntegration;
import com.comino.flight.panel.control.FlightControlPanel;
import com.comino.flight.tabs.FlightTabs;
import com.comino.flight.widgets.statusline.StatusLineWidget;
import com.comino.mav.control.IMAVController;
import com.comino.mav.control.impl.MAVSimController;
import com.comino.mav.control.impl.MAVUdpController;
import com.comino.model.file.FileHandler;
import com.comino.msp.log.MSPLogger;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

	private static IMAVController control = null;

	private static FileHandler fileHandler = null;

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
	private MenuBar menubar;


	public MainApp() {
		super();
	}

	@Override
	public void start(Stage primaryStage) {

		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("MAVGCL Analysis");

		String peerAddress = null;
        String proxy = null;

		Map<String,String> args = getParameters().getNamed();

		if(args.size()> 0) {
			peerAddress  = args.get("peerAddress");
			proxy = args.get("proxy");
		}

		if(peerAddress ==null) {
			control = new MAVSimController();
			control.connect();
		}
		else {
			if(peerAddress.contains("127.0") || peerAddress.contains("localhost")) {
				if(proxy==null)
				control = new MAVUdpController(peerAddress,14556,14550);
				else
					control = new MAVUdpController(peerAddress,14558,14550);
			}
			else {
				control = new MAVUdpController(peerAddress,14555,14550);
			}
		}

		AnalysisIntegration.registerFunction(control);
		MSPLogger.getInstance(control);
		FlightModeProperties.getInstance(control);

		fileHandler = new FileHandler(primaryStage,control);

		initRootLayout();
		showMAVGCLApplication();

	}

	@Override
	public void stop() throws Exception {
		control.close();
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
				fileHandler.fileImport();
				controlpanel.getRecordControl().refreshCharts();
			}

		});

		m_px4log.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				fileHandler.fileImportPX4Log();
				controlpanel.getRecordControl().refreshCharts();

			}

		});

		m_export.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(control.getCollector().getModelList().size()>0)
				   fileHandler.fileExport();
			}
		});
	}


	public void showMAVGCLApplication() {

		try {
			// Load person overview.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("tabs/MAVGCL2.fxml"));
			AnchorPane flightPane = (AnchorPane) loader.load();

			// Set person overview into the center of root layout.
			rootLayout.setCenter(flightPane);
			BorderPane.setAlignment(flightPane, Pos.TOP_LEFT);;


			StatusLineWidget statusline = new StatusLineWidget();
			rootLayout.setBottom(statusline);

			statusline.setup(control);

			controlpanel = new FlightControlPanel();
			rootLayout.setLeft(controlpanel);
			controlpanel.setup(control);


			if(!control.isConnected())
				control.connect();

			FlightTabs fvController = loader.getController();
			fvController.setup(controlpanel,statusline, control);
			fvController.setPrefHeight(820);
		//	fvController.prefHeightProperty().bind(rootLayout.heightProperty());



		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
