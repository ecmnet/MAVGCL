package com.comino.flight;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FirstPreloader extends Preloader {


    Stage stage;
    boolean noLoadingProgress = true;

    private Scene createPreloaderScene() {
    	ImageView splash = new ImageView(new Image(getClass().getResource("splash.png").toExternalForm()));
    	VBox splashLayout = new VBox();
        splashLayout.getChildren().addAll(splash);
        splashLayout.setEffect(new DropShadow());
        splashLayout.getChildren().addAll();
        Scene scene =  new Scene(splashLayout, 600, 263);
    	return scene;
    }

    public void start(Stage stage) throws Exception {
        this.stage = stage;
        stage.setScene(createPreloaderScene());
        stage.show();
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
            stage.hide();
        }
    }
 }