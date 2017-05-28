package com.comino.flight.ui.widgets.map;

import com.comino.flight.FXMLLoadHelper;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class TestMap extends Application {

	public static void main(String[] args) {
		launch(args);

	}

	public void init() throws Exception {

			FXMLLoadHelper.setApplication(this);
	}

	@Override
	public void start(Stage stage) throws Exception {

		FlowPane pane = new FlowPane();

		try {
		MapWidget map = new MapWidget();
		pane.getChildren().add(map);

		Scene scene = new Scene(pane);

		stage.setTitle("TestMap");
		stage.setScene(scene);
		stage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}


	}

	@Override public void stop() {
		System.exit(0);
	}


}
