/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de.
 *   All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.ui.tabs;

import java.util.HashMap;
import java.util.Map;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.ui.widgets.panel.ChartControlWidget;
import com.comino.flight.ui.widgets.panel.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.mav3d.VehicleModel;
import com.comino.mav3d.Xform;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.utils.BlockPoint2D;
import com.comino.msp.utils.MSPMathUtils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.effect.Light;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.util.Duration;



public class MAV3DViewTab extends Pane implements IChartControl {

	private Timeline task = null;
	private Timeline mapt = null;

	private final Group root = new Group();
	private final Xform world = new Xform();
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final Xform cameraXform = new Xform();
	private final Xform cameraXform2 = new Xform();
	private final Xform cameraXform3 = new Xform();
	private final Xform axisGroup = new Xform();
	private final Xform mapGroup = new Xform();
	private SubScene subScene;

	private VehicleModel vehicle = null;

	private static final double CAMERA_INITIAL_DISTANCE = -650;
	private static final double CAMERA_INITIAL_X_ANGLE = 70.0;
	private static final double CAMERA_INITIAL_Y_ANGLE = -350.0;
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 10000.0;
	private static final double AXIS_LENGTH = 500.0;

	private static final double CONTROL_MULTIPLIER = 0.1;
	private static final double SHIFT_MULTIPLIER = 10.0;
	private static final double MOUSE_SPEED = 0.1;
	private static final double ROTATION_SPEED = 2.0;
	private static final double TRACK_SPEED = 0.3;

	private Map<Integer,Box> blocks    = null;
	private PhongMaterial grayMaterial   = new PhongMaterial();
	private PhongMaterial groundMaterial = new PhongMaterial();

	double mousePosX;
	double mousePosY;
	double mouseOldX;
	double mouseOldY;
	double mouseDeltaX;
	double mouseDeltaY;

	private DataModel model;

	public MAV3DViewTab() {

		this.blocks = new HashMap<Integer,Box>();
		grayMaterial.setDiffuseColor(Color.BLUE);
		grayMaterial.setSpecularColor(Color.LIGHTBLUE);
		groundMaterial.setDiffuseColor(Color.LIGHTGRAY);

		FXMLLoadHelper.load(this, "MAV3DViewTab.fxml");
		task = new Timeline(new KeyFrame(Duration.millis(50), ae -> {
			Platform.runLater(() -> {
				vehicle.updateState(model);
			});
		} ) );
		task.setCycleCount(Timeline.INDEFINITE);
		task.play();

		mapt = new Timeline(new KeyFrame(Duration.millis(250), ae -> {
			Platform.runLater(() -> {
				for(int k=0;k<mapGroup.getChildren().size();k++)
					mapGroup.getChildren().get(k).setVisible(false);
				model.grid.getData().forEach((i,b) -> {
					getBlockBox(i,b).setVisible(true);;
				});
			});
		} ) );
		mapt.setCycleCount(Timeline.INDEFINITE);
		mapt.play();
	}

	@FXML
	private void initialize() {

		buildCamera();
		handleMouse(this);

		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);

		subScene = new SubScene(root,0,0,true,javafx.scene.SceneAntialiasing.BALANCED);
		subScene.fillProperty().set(Color.ALICEBLUE);
		subScene.setCamera(camera);
		subScene.widthProperty().bind(this.widthProperty().subtract(20));
		subScene.heightProperty().bind(this.heightProperty().subtract(20));
		subScene.setLayoutX(10);  subScene.setLayoutY(10);

		this.getChildren().add(subScene);

		Box ground = new Box(AXIS_LENGTH,0,AXIS_LENGTH);
		ground.setMaterial(groundMaterial);

	    AmbientLight ambient = new AmbientLight();
	    ambient.setColor(Color.WHITE);

	//	buildAxes();
		vehicle = new VehicleModel(16);
		world.getChildren().addAll(ground, mapGroup, vehicle, ambient);
	}


	public MAV3DViewTab setup(ChartControlWidget recordControl, IMAVController control) {
		this.model = control.getCurrentModel();

		return this;
	}


	@Override
	public FloatProperty getScrollProperty() {
		return null;
	}


	@Override
	public void refreshChart() {
		blocks.forEach((i,p) -> {
			Platform.runLater(() -> {
				mapGroup.getChildren().remove(p);
			});
		});
		blocks.clear();
	}


	@Override
	public IntegerProperty getTimeFrameProperty() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public BooleanProperty getIsScrollingProperty() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public KeyFigurePreset getKeyFigureSelection() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setKeyFigureSeletcion(KeyFigurePreset preset) {
		// TODO Auto-generated method stub

	}

	private void buildCamera() {
		root.getChildren().add(cameraXform);
		cameraXform.getChildren().add(cameraXform2);
		cameraXform2.getChildren().add(cameraXform3);
		cameraXform3.getChildren().add(camera);
		cameraXform3.setRotateZ(180.0);

		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
		cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
		cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
		camera.setVisible(true);
	}

	private void buildAxes() {
		final PhongMaterial redMaterial = new PhongMaterial();
		redMaterial.setDiffuseColor(Color.DARKRED);
		redMaterial.setSpecularColor(Color.RED);

		final PhongMaterial greenMaterial = new PhongMaterial();
		greenMaterial.setDiffuseColor(Color.DARKGREEN);
		greenMaterial.setSpecularColor(Color.GREEN);

		final PhongMaterial blueMaterial = new PhongMaterial();
		blueMaterial.setDiffuseColor(Color.DARKBLUE);
		blueMaterial.setSpecularColor(Color.BLUE);

		final Box xAxis = new Box(AXIS_LENGTH, 1, 1);
		final Box yAxis = new Box(1, AXIS_LENGTH, 1);
		final Box zAxis = new Box(1, 1, AXIS_LENGTH);

		xAxis.setMaterial(redMaterial);
		yAxis.setMaterial(greenMaterial);
		zAxis.setMaterial(blueMaterial);

		axisGroup.getChildren().addAll(xAxis, yAxis, zAxis);
		axisGroup.setVisible(true);
		world.getChildren().addAll(axisGroup);
	}

	private void handleMouse(final Node node) {

		node.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override public void handle(MouseEvent me) {
				mousePosX = me.getSceneX();
				mousePosY = me.getSceneY();
				mouseOldX = me.getSceneX();
				mouseOldY = me.getSceneY();
			}
		});
		node.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override public void handle(MouseEvent me) {
				mouseOldX = mousePosX;
				mouseOldY = mousePosY;
				mousePosX = me.getSceneX();
				mousePosY = me.getSceneY();
				mouseDeltaX = (mousePosX - mouseOldX);
				mouseDeltaY = (mousePosY - mouseOldY);

				double modifier = 1.0;

				if (me.isControlDown()) {
					modifier = CONTROL_MULTIPLIER;
				}
				if (me.isShiftDown()) {
					modifier = SHIFT_MULTIPLIER;
				}

				if (me.isPrimaryButtonDown()) {
					cameraXform.ry.setAngle(cameraXform.ry.getAngle() -
							mouseDeltaX*MOUSE_SPEED*modifier*ROTATION_SPEED);  //
							cameraXform.rx.setAngle(cameraXform.rx.getAngle() +
									mouseDeltaY*MOUSE_SPEED*modifier*ROTATION_SPEED);  // -
				}
			}
		});

		node.setOnZoom(event -> {
			camera.setTranslateZ(camera.getTranslateZ() + (event.getZoomFactor()-1)*MOUSE_SPEED*3000);
		});
	}



	private Box getBlockBox(int block, BlockPoint2D b) {

		if(blocks.containsKey(block))
			return blocks.get(block);

		final Box box = new Box(1, 1, 1);

		box.setTranslateX(-b.y*23);
		box.setTranslateY(0);
		box.setTranslateZ(b.x*23);
		box.setMaterial(grayMaterial);

		mapGroup.getChildren().addAll(box);
        blocks.put(block,box);

		return box;
	}

}
