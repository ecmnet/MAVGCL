package com.comino.flight;

import java.io.IOException;
import java.util.Map;

import com.comino.flight.analysis.FlightAnalysisController;
import com.comino.mav.control.IMAVController;
import com.comino.mav.control.impl.MAVSerialController;
import com.comino.mav.control.impl.MAVUdpController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

	  private IMAVController control = null;

	  private Stage primaryStage;
	  private BorderPane rootLayout;




	@Override
	public void start(Stage primaryStage) {

		this.primaryStage = primaryStage;
        this.primaryStage.setTitle("MAVGCL");

        String ipAddress = null;

        Map<String,String> args = getParameters().getNamed();

        if(args.size()> 0)
          ipAddress = args.get("udp");

        System.out.println(ipAddress);

        if(ipAddress==null)
        	control = new MAVSerialController();
        else
        	control = new MAVUdpController(ipAddress);

        if(!control.isConnected())
           control.connect();

        initRootLayout();
        showMAVGCLApplication();


	}

	@Override
	public void stop() throws Exception {
		control.stop();
		control.close();
		super.stop();
		System.exit(-1);
	}



	public static void main(String[] args) {
		launch(args);
	}

	public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("analysis/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            scene.getStylesheets().add(getClass().getResource("analysis/application.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public void showMAVGCLApplication() {



        try {
            // Load person overview.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("analysis/MAVGAnalysis.fxml"));
            AnchorPane flightPane = (AnchorPane) loader.load();

            // Set person overview into the center of root layout.
            rootLayout.setCenter(flightPane);

            FlightAnalysisController fvController = loader.getController();
            fvController.start(control);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
