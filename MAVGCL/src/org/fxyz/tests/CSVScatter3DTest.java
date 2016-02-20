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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javafx.animation.AnimationTimer;
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
import javafx.stage.Stage;
import org.fxyz.cameras.CameraTransformer;
import org.fxyz.geometry.Point3D;
import org.fxyz.shapes.primitives.ScatterMesh;
import org.fxyz.utils.Palette;

/**
 *
 * @author jpereda
 */
public class CSVScatter3DTest extends Application {
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
        
//        // create some data
//        IntStream.range(0,100000)
//                .forEach(i->data.add(new Point3D((float)(30*Math.sin(50*i)),
//                                    (float)(Math.sin(i)*(100+30*Math.cos(100*i))),
//                                    (float)(Math.cos(i)*(100+30*Math.cos(200*i))),
//                                    i))); // <-- f
//        // and write to csv file
//        Path out = Paths.get("output.txt");
//        Files.write(out,data.stream().map(p3d->p3d.toCSV()).collect(Collectors.toList()),Charset.defaultCharset());
        
        
        // read from csv file
        Path out = getCSVFile(0);
        if(out!=null){
            Files.lines(out).map(s->s.split(";")).forEach(s->data.add(new Point3D(Float.parseFloat(s[0]),
                    Float.parseFloat(s[1]),Float.parseFloat(s[2]),Float.parseFloat(s[3]))));
        }
        
        ScatterMesh scatter = new ScatterMesh(data, true, 1, 0);
    // DENSITY
        // texture is given by p.f value, don't change this!
        scatter.setTextureModeVertices3D(1530,p->p.f);
        
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
        
        final boolean constantVertices = true;
        lastEffect = System.nanoTime();
        AtomicInteger count=new AtomicInteger(0);
        
        List<List<Number>> fullData=new ArrayList<>();
        
        if(constantVertices) {
            // if possible we can cache all the data
            Stream.of(0,1,2,3,4,3,2,1).forEach(i->{
                Path out2 = getCSVFile(i);
                if(out2 != null) {
                    try {
                        List<Number> data2= new ArrayList<>();
                        Files.lines(out2).map(s->s.split(";"))
                            .forEach(s->{
                                float f = Float.parseFloat(s[3]);
                                // 4 vertices tetrahedra
                                data2.add(f);
                                data2.add(f);
                                data2.add(f);
                                data2.add(f);
                            });
                        fullData.add(data2);
                    } catch (IOException ex) {}
                }
            });
        }
        
        AnimationTimer timerEffect = new AnimationTimer() {

            @Override
            public void handle(long now) {
                if (now > lastEffect + 50_000_000l) {
                    try {
//                        long t=System.currentTimeMillis();
                        if(constantVertices && fullData != null){
                            // Vertices coordinates are always the same: mesh is tha same, we only
                            // need to update F on each element
                            scatter.setFunctionData(fullData.get(count.get()%8));
//                            System.out.println("t "+(System.currentTimeMillis()-t));
                        } else {
                            // vertices coordinates may change in time, we need to create them all over again reading the files:
                            Path out2 = getCSVFile((int)(Stream.of(0,1,2,3,4,3,2,1).toArray()[count.get()%8]));
                            if(out2 != null) {
                                List<Point3D> data2= new ArrayList<>();
                                Files.lines(out2).map(s->s.split(";")).forEach(s->data2.add(new Point3D(Float.parseFloat(s[0]),
                                        Float.parseFloat(s[1]),Float.parseFloat(s[2]),Float.parseFloat(s[3]))));
                                scatter.setScatterData(data2);
                                scatter.setTextureModeVertices1D(1530,p->p);
                            }
//                            System.out.println("t "+(System.currentTimeMillis()-t));
                        }
                    } catch (IOException ex) {
                    }
                    
                    count.getAndIncrement();
                    lastEffect = now;
                }
            }
        };
        timerEffect.start();
        
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }    
    
    private Path getCSVFile(int i) {
        try {
            return Paths.get(getClass().getResource("res/csv/input_"+i+".txt").toURI());
        } catch (URISyntaxException ex) {
        }
        return null;
    }
    
}
