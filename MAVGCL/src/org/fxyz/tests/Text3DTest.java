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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxyz.cameras.CameraTransformer;
import org.fxyz.geometry.Point3D;
import org.fxyz.shapes.primitives.CuboidMesh;
import org.fxyz.shapes.primitives.Text3DMesh;
import org.fxyz.shapes.primitives.TexturedMesh;
import org.fxyz.shapes.primitives.TriangulatedMesh;
import org.fxyz.utils.OBJWriter;
import org.fxyz.utils.Palette;
import org.fxyz.utils.Palette.ColorPalette;
import org.fxyz.utils.Patterns;

/**
 *
 * @author jpereda
 */
public class Text3DTest extends Application {
    private PerspectiveCamera camera;
    private final double sceneWidth = 600;
    private final double sceneHeight = 600;
    private final CameraTransformer cameraTransform = new CameraTransformer();
    
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;
//    private Function<Point3D, Number> dens = p->p.magnitude();
    private long lastEffect;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Group sceneRoot = new Group();
        Scene scene = new Scene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.BLACK);
        camera = new PerspectiveCamera(true);        
     
        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateX(800);
        camera.setTranslateZ(-3000);
//        cameraTransform.ry.setAngle(-25.0);
        cameraTransform.rx.setAngle(10.0);
        //add a Point Light for better viewing of the grid coordinate system
//        PointLight light1 = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(new AmbientLight());
        scene.setCamera(camera);
        
        Group group = new Group(cameraTransform);   
        Text3DMesh letters = new Text3DMesh("3DMesh","Gadugi Bold",400,true,120,0,1);
//        letters.setDrawMode(DrawMode.LINE);
        // NONE
//        letters.setTextureModeNone(Color.ROYALBLUE);
    // IMAGE
//        letters.setTextureModeImage(getClass().getResource("res/steel-background1.jpg").toExternalForm());
//        letters.setTextureModeImage(getClass().getResource("res/marvel1.jpg").toExternalForm());
    // DENSITY
        letters.setTextureModeVertices3D(1530,p->p.magnitude());
//        letters.setTextureModeVertices3D(1530,p->Math.sin(p.y/50)*Math.cos(p.x/40)*p.z);
    // FACES
//        letters.setTextureModeFaces(Palette.ColorPalette.HSB,16);
        group.getChildren().add(letters);         
        sceneRoot.getChildren().addAll(group);    
        
        //First person shooter keyboard movement 
        scene.setOnKeyPressed(event -> {
            double change = 10.0;
            //Add shift modifier to simulate "Running Speed"
            if(event.isShiftDown()) { change = 50.0; }
            //What key did the user press?
            KeyCode keycode = event.getCode();
            //Step 2c: Add Zoom controls
            if(keycode == KeyCode.W) { camera.setTranslateZ(camera.getTranslateZ() + change); }
            if(keycode == KeyCode.S) { camera.setTranslateZ(camera.getTranslateZ() - change); }
            //Step 2d:  Add Strafe controls
            if(keycode == KeyCode.A) { camera.setTranslateX(camera.getTranslateX() - change); }
            if(keycode == KeyCode.D) { camera.setTranslateX(camera.getTranslateX() + change); }
        });        
        
        scene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        scene.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);
            
            double modifier = 10.0;
            double modifierFactor = 0.1;
            
            if (me.isControlDown()) {
                modifier = 0.1;
            }
            if (me.isShiftDown()) {
                modifier = 50.0;
            }
            if (me.isPrimaryButtonDown()) {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);  // +
                cameraTransform.rx.setAngle(((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);  // -
            } else if (me.isSecondaryButtonDown()) {
                double z = camera.getTranslateZ();
                double newZ = z + mouseDeltaX * modifierFactor * modifier;
                camera.setTranslateZ(newZ);
            } else if (me.isMiddleButtonDown()) {
                cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3);  // -
                cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3);  // -
            }
        });
               
        primaryStage.setTitle("F(X)yz - Text3D");
        primaryStage.setScene(scene);
        primaryStage.show();     
        
        // Letter transformations
        
        IntStream.range(0,letters.getChildren().size()) 
                .forEach(i->{
                    double y=(((double)i)/((double)letters.getChildren().size())*3d*Math.PI);
                    ((TexturedMesh)(letters.getChildren().get(i))).getTranslate().setY(100d*Math.sin(y));
//                    ((TexturedMesh)(letters.getChildren().get(i))).getRotateZ().setAngle(Math.cos(y)*180d/Math.PI);
//                    ((TexturedMesh)(letters.getChildren().get(i))).getRotateX().setAngle(Math.cos(y)*180d/Math.PI);
                });
        
        // Letter animations
        
        final Timeline rotateEffect1 = new Timeline();
        rotateEffect1.setCycleCount(Timeline.INDEFINITE);
        TexturedMesh t0 = letters.getMeshFromLetter("M");
        final KeyValue kv1 = new KeyValue(t0.getRotateY().angleProperty(), 360);
        final KeyFrame kf1 = new KeyFrame(Duration.millis(3000), kv1);
        rotateEffect1.getKeyFrames().addAll(kf1);
        rotateEffect1.play();
        
        final Timeline rotateEffect2 = new Timeline();
        TexturedMesh t1 = letters.getMeshFromLetter("3");
        final KeyValue kv2 = new KeyValue(t1.getRotateX().angleProperty(), 360);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(2000), kv2);
        rotateEffect2.getKeyFrames().addAll(kf2);
        rotateEffect2.play();
//        
        final Timeline rotateEffect3 = new Timeline();
        rotateEffect3.setCycleCount(Timeline.INDEFINITE);
        TexturedMesh t5 = letters.getMeshFromLetter("h");
        final KeyValue kv1x = new KeyValue(t5.getScale().xProperty(), 1.2, Interpolator.EASE_BOTH);
        final KeyValue kv1y = new KeyValue(t5.getScale().yProperty(), 1.2, Interpolator.EASE_BOTH);
        final KeyValue kv1z = new KeyValue(t5.getScale().zProperty(), 1.2, Interpolator.EASE_BOTH);
        final KeyFrame kfs1 = new KeyFrame(Duration.millis(500), kv1x,kv1y,kv1z);
        final KeyValue kv2x = new KeyValue(t5.getScale().xProperty(), 0.3, Interpolator.EASE_BOTH);
        final KeyValue kv2y = new KeyValue(t5.getScale().yProperty(), 0.3, Interpolator.EASE_BOTH);
        final KeyValue kv2z = new KeyValue(t5.getScale().zProperty(), 0.3, Interpolator.EASE_BOTH);
        final KeyFrame kfs2 = new KeyFrame(Duration.millis(2000), kv2x,kv2y,kv2z);
        final KeyValue kv3x = new KeyValue(t5.getScale().xProperty(), 1, Interpolator.EASE_BOTH);
        final KeyValue kv3y = new KeyValue(t5.getScale().yProperty(), 1, Interpolator.EASE_BOTH);
        final KeyValue kv3z = new KeyValue(t5.getScale().zProperty(), 1, Interpolator.EASE_BOTH);
        final KeyFrame kfs3 = new KeyFrame(Duration.millis(3000), kv3x,kv3y,kv3z);
        rotateEffect3.getKeyFrames().addAll(kfs1,kfs2,kfs3);
        rotateEffect3.play();
        
        lastEffect = System.nanoTime();
        AtomicInteger count=new AtomicInteger(1);
        AnimationTimer timerEffect = new AnimationTimer() {

            @Override
            public void handle(long now) {
                if (now > lastEffect + 1_000_000_000l) {
                    System.out.println("*** "+count.get());
                    letters.setFont(Font.getFontNames().get(count.get()%Font.getFontNames().size()));
                    if(count.get()%10<2){
                        letters.setTextureModeNone(Color.hsb(count.get()%360, 1, 1));
                    } else if(count.get()%10<4){
                        letters.setTextureModeImage(getClass().getResource("res/steel-background1.jpg").toExternalForm());
                    } else if(count.get()%10<8){
                        letters.setTextureModeVertices3D(1530,p->(double)(p.y/(20+count.get()%20))*(p.x/(10+count.get()%10)));
                    } else {
                        letters.setTextureModeFaces(1530);
                    }
                    
                    if(count.get()%20>15){
                        letters.setDrawMode(DrawMode.LINE);
                    } else {
                        letters.setDrawMode(DrawMode.FILL);
                    }
                    letters.getChildren().forEach(m->((TexturedMesh)m).setDensity(p->p.magnitude()*Math.cos(p.y/100d*(count.get()%5))));
                    count.getAndIncrement();
                    lastEffect = now;
                }
            }
        };
//        timerEffect.start();
        OBJWriter writer=new OBJWriter((TriangleMesh)((TexturedMesh)(letters.getChildren().get(0))).getMesh(),
                "letter");
        writer.setTextureColors(6);
        //writer.setTexturePattern();
//        writer.setTextureImage(getClass().getResource("res/LaminateSteel.jpg").toExternalForm());
        writer.exportMesh();
        
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }    
    
}
