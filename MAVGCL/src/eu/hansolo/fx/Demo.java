/*
 * Copyright (c) 2013 by Gerrit Grunwald
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

package eu.hansolo.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


/**
 * User: hansolo
 * Date: 02.04.14
 * Time: 07:50
 */
public class Demo extends Application {

    private AirCompass compass;
    
    @Override public void init() {
        compass = new AirCompass();
        compass.setAnimated(false);
    }

    @Override public void start(Stage stage) {
        StackPane pane = new StackPane();
        pane.getChildren().addAll(compass);

        Scene scene = new Scene(pane);

        stage.setScene(scene);
        stage.show();
        
        compass.setBearing(90);
        compass.setPlaneColor(Color.RED);
        compass.setOrientationColor(Color.YELLOW);
    }

    @Override public void stop() {

    }

    public static void main(String[] args) {
        launch(args);
    }
}
