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

import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.scene.CameraHelper;
import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import org.fxyz.cameras.CameraTransformer;

/**
 *
 * @author Jason Pollastrini aka jdub1581
 */
public class Drag3DObject extends Application {

    private static final boolean RUN_JASON=false;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /*
     *  
     Add to Root ->-> Run ->-> Enjoy!
    
     Depends how you want to move your object, but assuming this :

     when the mouse moves up or down, the object moves vertically parallel to the screen.

     when the mouse moves left or right, the object moves horizontally parallel to the screen.

     To do this, you only need a couple of informations.

     1. Relative displacement of the mouse.
     2. The Up and Right axis of the view.

     When you got that, if your mouse as some horizontal movement you do :

     objPos += mouseMovementx*scale*RightAxis

     scale maps the screen mouse coordinates to your software display units.

     Simply replace mouseMovementX by mouseMovementY and RightAxis by UpAxis to get vertical motion.
    
    
     scale could simply be the ratio of viewvolume dimension over the screen dimension. This has some problem if you are using a perspective projection, because if your object is far away the ratio will be incorrect because the dimension are bigger and the object won't stay under the mouse. But it work correctly for an ortho projection.

     Some concrete numbers. Say you are using an ortho project with with following values :

     left = -100, right = 100, top = 100, bottom = -100, near and far do not matter. The view volume dimension will always be :

     horizontally abs( left - right ) = 200 in our case
     vertically abs( top - bottom ) = 200 in our case

     And your screen dimension are 1024 X 768. So our movement scale is horizontally = 200 / 1024 and 200/768.

     Great, now if your mouse moves 300 units to the left and 100 units up.
    
     objPos += ( cameraRightAxis*300*200/1024 ) + ( cameraUpAxis*100*200/1024 )
    
     That is for an ortho projection. 
    
     For a perspective projection, we need to adjust the scale depending on the distance of the object to the near plane. I won't put any number, it should become obvious now.
     ViewFrustum = ( left, right, bottom, top, znear, zfar )
    
     XScale = abs( left - right ) / XScreenDimension * ( distance( object, camera )  / znear )
     YScale = abs( top - bottom ) / YScreenDimension * ( distance( object, camera )  / znear )
     */
    @Override
    public void start(Stage stage) {
        Box floor = new Box(1000, 10, 1000);
        floor.setTranslateY(150);
        root.getChildren().add(floor);

        Sphere s = new Sphere(150);
        s.setTranslateX(5);
        s.setPickOnBounds(true);
        root.getChildren().add(s);
        
        showStage(stage);
    }

    /**
     * *************************************************************************
     ************************ Stage Default Setup ****************************
     *///***********************************************************************
    private Group root = new Group();
    private PerspectiveCamera camera;
    private final double sceneWidth = 1024;
    private final double sceneHeight = 768;
    private final CameraTransformer cameraTransform = new CameraTransformer();

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    private void showStage(Stage stage) {
        Scene scene = new Scene(root, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.web("3d3d3d"));

        loadCamera(scene);
        loadControls(scene);

        stage.setTitle("F(X)yz Sample: 3D Dragging");
        stage.setScene(scene);
        stage.show();
    }

    private void loadCamera(Scene scene) {
        //initialize camera
        camera = new PerspectiveCamera(true);
        camera.setVerticalFieldOfView(RUN_JASON);

        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(-5000);
        cameraTransform.ry.setAngle(0.0);
        cameraTransform.rx.setAngle(-45.0);

        //add a Point Light for better viewing of the grid coordinate system
        PointLight light = new PointLight(Color.GAINSBORO);
        cameraTransform.getChildren().add(light);
        cameraTransform.getChildren().add(new AmbientLight(Color.WHITE));
        light.setTranslateX(camera.getTranslateX());
        light.setTranslateY(camera.getTranslateY());
        light.setTranslateZ(camera.getTranslateZ());
        //attach camera to scene
        scene.setCamera(camera);

    }
    
    private volatile boolean isPicking=false;
    private Vec3d vecIni, vecPos;
    private double distance;
    private Sphere s;
    
    private void loadControls(Scene scene) {
        //First person shooter keyboard movement 
        scene.setOnKeyPressed(event -> {
            double change = 10.0;
            //Add shift modifier to simulate "Running Speed"
            if (event.isShiftDown()) {
                change = 50.0;
            }
            //What key did the user press?
            KeyCode keycode = event.getCode();
            //Step 2c: Add Zoom controls
            if (keycode == KeyCode.W) {
                camera.setTranslateZ(camera.getTranslateZ() + change);
            }
            if (keycode == KeyCode.S) {
                camera.setTranslateZ(camera.getTranslateZ() - change);
            }
            //Step 2d:  Add Strafe controls
            if (keycode == KeyCode.A) {
                camera.setTranslateX(camera.getTranslateX() - change);
            }
            if (keycode == KeyCode.D) {
                camera.setTranslateX(camera.getTranslateX() + change);
            }
        });

        scene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
            PickResult pr = me.getPickResult();
            if(pr!=null && pr.getIntersectedNode() != null && pr.getIntersectedNode() instanceof Sphere){
                distance=pr.getIntersectedDistance();
                s = (Sphere) pr.getIntersectedNode();
                isPicking=true;
                vecIni = unProjectDirection(mousePosX, mousePosY, scene.getWidth(),scene.getHeight());
            }
        });
        scene.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);
            if(RUN_JASON){
                //objPos += mouseMovementx*scale*RightAxis
                if (isPicking) {
                    if (mousePosX < mouseOldX) {
                        System.out.println("moving left");
                        Point3D pos = CameraHelper.pickProjectPlane(camera, mousePosX, mousePosY);
                        s.setTranslateX(s.getTranslateX() + (pos.getX() - mouseOldX) * camera.getNearClip());

                        return;
                    } else if (mousePosX > mouseOldX) {
                        System.err.println("moving right");
                        Point3D pos = CameraHelper.pickProjectPlane(camera, mousePosX, mousePosY);
                        s.setTranslateX(s.getTranslateX() - (pos.getX() - mouseOldX) * camera.getNearClip());

                        return;
                    }

                    if (mousePosY < mouseOldY) {
                        System.out.println("moving up");
                    } else if (mousePosY > mouseOldY) {
                        System.err.println("moving down");
                    }     
                    return;
                }
            } else {
                if(isPicking){
                    double modifier = (me.isControlDown()?0.2:me.isAltDown()?2.0:1)*(30d/camera.getFieldOfView());
                    modifier *=(30d/camera.getFieldOfView());
                    vecPos = unProjectDirection(mousePosX, mousePosY, scene.getWidth(),scene.getHeight());
                    Point3D p=new Point3D(distance*(vecPos.x-vecIni.x),
                                distance*(vecPos.y-vecIni.y),distance*(vecPos.z-vecIni.z));
                    s.getTransforms().add(new Translate(modifier*p.getX(),0,modifier*p.getZ()));
                    vecIni=vecPos;

                } else {
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
                }
            }
        });
        scene.setOnMouseReleased((MouseEvent me)->{
            if(isPicking){
                isPicking=false;
            }
        });

    }//*************************************************************************
    //****************************  End Default  *******************************
    //**************************************************************************

    /*
     From fx83dfeatures.Camera3D
     http://hg.openjdk.java.net/openjfx/8u-dev/rt/file/f4e58490d406/apps/toys/FX8-3DFeatures/src/fx83dfeatures/Camera3D.java
    */
    /*
     * returns 3D direction from the Camera position to the mouse
     * in the Scene space 
     */
    
    public Vec3d unProjectDirection(double sceneX, double sceneY, double sWidth, double sHeight) {
        double tanHFov = Math.tan(Math.toRadians(camera.getFieldOfView()) * 0.5f);
        Vec3d vMouse = new Vec3d(2*sceneX/sWidth-1, 2*sceneY/sWidth-sHeight/sWidth, 1);
        vMouse.x *= tanHFov;
        vMouse.y *= tanHFov;

        Vec3d result = localToSceneDirection(vMouse, new Vec3d());
        result.normalize();
        return result;
    }
    
    public Vec3d localToScene(Vec3d pt, Vec3d result) {
        Point3D res = camera.localToParentTransformProperty().get().transform(pt.x, pt.y, pt.z);
        if (camera.getParent() != null) {
            res = camera.getParent().localToSceneTransformProperty().get().transform(res);
        }
        result.set(res.getX(), res.getY(), res.getZ());
        return result;
    }
    
    public Vec3d localToSceneDirection(Vec3d dir, Vec3d result) {
        localToScene(dir, result);
        result.sub(localToScene(new Vec3d(0, 0, 0), new Vec3d()));
        return result;
    }
}//==========================================================================EOF
