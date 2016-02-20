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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxyz.cameras.CameraTransformer;
import org.fxyz.geometry.Point3D;
import org.fxyz.shapes.primitives.ScatterMesh;
import org.fxyz.shapes.primitives.TexturedMesh;
import org.fxyz.utils.OBJWriter;

/**
 *
 * @author jpereda
 */
public class Scatter3DTest extends Application {
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
        camera.setTranslateX(0);
        camera.setTranslateZ(-1000);
        cameraTransform.ry.setAngle(-25.0);
        cameraTransform.rx.setAngle(-10.0);
        //add a Point Light for better viewing of the grid coordinate system
        PointLight light = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(new AmbientLight());
        light.setTranslateX(camera.getTranslateX());
        light.setTranslateY(camera.getTranslateY());
        light.setTranslateZ(camera.getTranslateZ());        
        scene.setCamera(camera);
        long time=System.currentTimeMillis();
        Group group = new Group(cameraTransform);   
        List<Point3D> data= new ArrayList<>();
        IntStream.range(0,100000)
                .forEach(i->data.add(new Point3D((float)(30*Math.sin(50*i)),
                                    (float)(Math.sin(i)*(100+30*Math.cos(100*i))),
                                    (float)(Math.cos(i)*(100+30*Math.cos(200*i))),
                                    i))); // <-- f
        ScatterMesh scatter = new ScatterMesh(data, true, 1, 0);
        // NONE
//        scatter.setTextureModeNone(Color.ROYALBLUE);
//        scatter.setDrawMode(DrawMode.LINE);
    // IMAGE
//        scatter.setTextureModeImage(getClass().getResource("res/steel-background1.jpg").toExternalForm());
//        scatter.setTextureModeImage(getClass().getResource("res/share-carousel2.jpg").toExternalForm());
    // DENSITY
        scatter.setTextureModeVertices3D(1530,p->p.magnitude());
//        scatter.setTextureModeVertices1D(1530,p->Math.sqrt(p.doubleValue()));
//        scatter.setTextureModeVertices3D(1530,p->Math.sin(p.y/50)*Math.cos(p.x/40)*p.z);
    // FACES
//        scatter.setTextureModeFaces(Palette.ColorPalette.HSB,1530);
        group.getChildren().add(scatter);         
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
               
        primaryStage.setTitle("F(X)yz - ScatterMesh Test");
        primaryStage.setScene(scene);
        primaryStage.show();      
        
        System.out.println("time "+(System.currentTimeMillis()-time));
        final Timeline rotateEffect2 = new Timeline();
        TexturedMesh t1 = scatter.getMeshFromId("1");
        final KeyValue kv2 = new KeyValue(t1.getRotateX().angleProperty(), 360);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(2000), kv2);
        rotateEffect2.getKeyFrames().addAll(kf2);
//        rotateEffect2.play();
//        
        final Timeline rotateEffect3 = new Timeline();
        rotateEffect3.setCycleCount(Timeline.INDEFINITE);
        TexturedMesh t5 = scatter.getMeshFromId("2");
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
//        rotateEffect3.play();
        
        final boolean constantVertices = true;
        lastEffect = System.nanoTime();
        AtomicInteger count=new AtomicInteger(0);
        AnimationTimer timerEffect = new AnimationTimer() {

            @Override
            public void handle(long now) {
                if (now > lastEffect + 50_000_000l) {
//                    if(count.get()%10<2){
//                        scatter.setTextureModeNone(Color.hsb(count.get()%360, 1, 1));
//                    } else if(count.get()%10<4){
//                        scatter.setTextureModeImage(getClass().getResource("res/steel-background1.jpg").toExternalForm());
//                    } else if(count.get()%10<8){
//                        scatter.setTextureModeVertices3D(1530,p->(double)(p.y/(20+count.get()%20))*(p.x/(10+count.get()%10)));
//                    } else {
//                        scatter.setTextureModeFaces(1530);
//                    }
//                    
//                    if(count.get()%20>15){
//                        scatter.setDrawMode(DrawMode.LINE);
//                    } else {
//                        scatter.setDrawMode(DrawMode.FILL);
//                    }
//                    scatter.getChildren().forEach(m->((TexturedMesh)m).setDensity(p->p.magnitude()*Math.cos(p.y/100d*(count.get()%5))));
                    count.getAndIncrement();
                    lastEffect = now;
                }
            }
        };
//        timerEffect.start();
        OBJWriter writer=new OBJWriter((TriangleMesh)((TexturedMesh)(scatter.getChildren().get(0))).getMesh(),
                "scatter");
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
