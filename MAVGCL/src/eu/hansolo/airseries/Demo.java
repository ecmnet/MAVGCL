/*
 * Copyright (c) 2014 by Gerrit Grunwald
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

package eu.hansolo.airseries;

import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


/**
 * User: hansolo
 * Date: 18.04.14
 * Time: 11:33
 */
public class Demo extends Application {
    private static final Random RND = new Random();
    
    private AirCompass     compass;
    private Horizon        horizon;
    private Altimeter      altimeter;

    private long           lastTimerCall;
    private AnimationTimer timer;

    @Override public void init() {
        compass   = new AirCompass();
        horizon   = new Horizon();
        altimeter = new Altimeter();
        
        lastTimerCall = System.nanoTime();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now > lastTimerCall + 5_000_000_000l) {
                    compass.setBearing(RND.nextInt(360));
                    horizon.setPitch(RND.nextInt(90) - 45);
                    horizon.setRoll(RND.nextInt(90) - 45);
                    altimeter.setValue(RND.nextInt(20000));
                    lastTimerCall = now;
                }
            }
        };
    }

    @Override public void start(Stage stage) {
        HBox pane = new HBox(compass, horizon, altimeter);
        pane.setPadding(new Insets(20, 20, 20, 20));
        pane.setSpacing(20);
        pane.setBackground(new Background(new BackgroundFill(Color.rgb(31, 31, 31), CornerRadii.EMPTY, Insets.EMPTY)));
        
        Scene scene = new Scene(pane);

        stage.setTitle("AirSeries");
        stage.setScene(scene);
        stage.show();
        
        timer.start();
    }

    @Override public void stop() {

    }

    public static void main(String[] args) {
        launch(args);
    }
}
