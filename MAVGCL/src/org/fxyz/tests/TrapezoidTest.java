/*
 * Copyright (C) 2013-2015 F(X)yz, 
 * Sean Phillips, Jason Pollastrini and Jose Pereda
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fxyz.tests;

import java.util.Random;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import org.fxyz.cameras.AdvancedCamera;
import org.fxyz.cameras.controllers.FPSController;
import org.fxyz.shapes.Octahedron;
import org.fxyz.shapes.Trapezoid;

/**
 *
 * @author Birdasaur
 * @adapted JDub1581's Capsule Test for ShapeContainer Classes 
 * 
 */
public class TrapezoidTest extends Application {
        
    private AdvancedCamera camera;
    private FPSController controller;
    private Group shapeGroup = new Group();        
    private final Group root = new Group();
   
    @Override
    public void start(Stage stage) {
        shapeGroup.getChildren().clear();
        generateShapes();
        root.getChildren().add(shapeGroup);
                
        camera = new AdvancedCamera();
        controller = new FPSController();
        
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setFieldOfView(42);
        camera.setController(controller);
        
        Scene scene = new Scene(new StackPane(root), 1024, 668, true, SceneAntialiasing.BALANCED);
        scene.setCamera(camera);
        scene.setFill(Color.BLACK);
        
        controller.setScene(scene);
        scene.setOnKeyPressed(event -> {
            //What key did the user press?
            KeyCode keycode = event.getCode();
            if(keycode == KeyCode.SPACE) {
                shapeGroup.getChildren().clear();
                generateShapes();                
            }
        });
        
        stage.setTitle("Random Trapezoids!");
        stage.setScene(scene);
        stage.show();
        stage.setFullScreen(false);
        stage.setFullScreenExitHint("");
    }
    private void generateShapes() {
        for (int i = 0; i < 50; i++) {
            Random r = new Random();
            //A lot of magic numbers in here that just artificially constrain the math
            double randomSmallSize = (double) ((r.nextDouble()*50) + 10);
            double randomBigSize = (double) ((r.nextDouble()*100)+ 50);
            double randomHeight = (double) ((r.nextDouble()*50)+ 25);
            double randomDepth = (double) ((r.nextDouble()*50) + 25);

            Color randomColor = new Color(r.nextDouble(), r.nextDouble(), r.nextDouble(), r.nextDouble());
            
            Trapezoid trapezoid = new Trapezoid(randomSmallSize, randomBigSize, 
                                    randomHeight, randomDepth, randomColor);
//            Trapezoid trapezoid = new Trapezoid(50, 100, 50, 50, randomColor);
            trapezoid.setEmissiveLightingColor(randomColor);
            trapezoid.setEmissiveLightingOn(r.nextBoolean());
            trapezoid.setDrawMode(r.nextBoolean() ? DrawMode.FILL : DrawMode.LINE);
            
            double translationX = Math.random() * 1024;
            if (Math.random() >= 0.5) {
                translationX *= -1;
            }
            double translationY = Math.random() * 1024;
            if (Math.random() >= 0.5) {
                translationY *= -1;
            }
            double translationZ = Math.random() * 1024;
            if (Math.random() >= 0.5) {
                translationZ *= -1;
            }
            Translate translate = new Translate(translationX, translationY, translationZ);
            Rotate rotateX = new Rotate(Math.random() * 360, Rotate.X_AXIS);
            Rotate rotateY = new Rotate(Math.random() * 360, Rotate.Y_AXIS);
            Rotate rotateZ = new Rotate(Math.random() * 360, Rotate.Z_AXIS);

            trapezoid.getTransforms().addAll(translate, rotateX, rotateY, rotateZ);
            
            shapeGroup.getChildren().add(trapezoid);
        }
    }    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}