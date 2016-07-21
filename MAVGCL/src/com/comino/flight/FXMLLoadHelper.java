package com.comino.flight;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class FXMLLoadHelper {

	public static int count = 0;
	private static Application app = null;

	public static void setApplication(Application application) {
		app = application;
	}

	public static void load(Node obj,String fxml) {
		FXMLLoader fxmlLoader = new FXMLLoader(obj.getClass().getResource(fxml));
		fxmlLoader.setRoot(obj);
		fxmlLoader.setController(obj);
		try {
			fxmlLoader.load();
			System.out.print(".");
			app.notifyPreloader(new ProgressNotification(count++));
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

}
