package com.comino.flight;

import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FirstPreloader extends Preloader {


    Stage stage;
    ProgressBar bar;
    boolean noLoadingProgress = true;

    private Scene createPreloaderScene() {
    	VBox splashLayout = new VBox();
    	splashLayout.getStylesheets().add(getClass().getResource("splash.css").toExternalForm());
    	ImageView splash = new ImageView(new Image(getClass().getResource("splash.png").toExternalForm()));
    	bar = new ProgressBar(0);
    	bar.setPrefHeight(5); bar.setPrefWidth(600);
        splashLayout.getChildren().addAll(splash,bar);
        splashLayout.setEffect(new DropShadow());
        splashLayout.getChildren().addAll();
        Scene scene =  new Scene(splashLayout, 600, 268);
    	return scene;
    }

    public void start(Stage stage) throws Exception {
        this.stage = stage;
        stage.setScene(createPreloaderScene());
        stage.setAlwaysOnTop(true);
        stage.show();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int max = 200;
                for (int i = 1; i <= max; i++) {
                	Thread.sleep(300/max);
                    bar.setProgress((double) i/max);
                }
                return null;
            }
        };
        Thread th = new Thread(task);
		th.setPriority(Thread.MAX_PRIORITY);
		th.setDaemon(true);
		th.start();
    }


    @Override
    public void handleStateChangeNotification(StateChangeNotification evt) {
        //ignore, hide after application signals it is ready
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification pn) {
        if (pn instanceof ProgressNotification) {
//           //expect application to send us progress notifications
//           //with progress ranging from 0 to 1.0
//           double v = ((ProgressNotification) pn).getProgress();
//           if (!noLoadingProgress) {
//               //if we were receiving loading progress notifications
//               //then progress is already at 50%.
//               //Rescale application progress to start from 50%
//               v = 0.5 + v/2;
//           }
//           bar.setProgress(v);
        } else if (pn instanceof StateChangeNotification) {
            //hide after get any state update from application
        	bar.setProgress(1);
            stage.hide();
        }
    }

    public static void main(String[] args) {
		launch(args);
	}
 }