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

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.ui.widgets.panel.ChartControlWidget;
import com.comino.flight.ui.widgets.panel.IChartControl;
import com.comino.flight.ui.widgets.view3D.objects.Camera;
import com.comino.flight.ui.widgets.view3D.objects.MapGroup;
import com.comino.flight.ui.widgets.view3D.objects.VehicleModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.util.Duration;



public class MAV3DViewTab extends Pane implements IChartControl {


	private Timeline task = null;

	private final Group root = new Group();
	private final Xform world = new Xform();

	private SubScene subScene;

	private VehicleModel vehicle = null;
	private Box ground = null;

	private static final double AXIS_LENGTH = 5000.0;

	private PhongMaterial groundMaterial = new PhongMaterial();

	private DataModel model;

	private MapGroup map;

	private Camera camera;

	public MAV3DViewTab() {
		System.setProperty("prism.dirtyopts", "false");

		groundMaterial.setDiffuseColor(Color.LIGHTGRAY);

		FXMLLoadHelper.load(this, "MAV3DViewTab.fxml");

	}

	@FXML
	private void initialize() {

		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);

		subScene = new SubScene(root,0,0,true,javafx.scene.SceneAntialiasing.BALANCED);
		subScene.fillProperty().set(Color.ALICEBLUE);
		subScene.widthProperty().bind(this.widthProperty().subtract(20));
		subScene.heightProperty().bind(this.heightProperty().subtract(20));
		subScene.setLayoutX(10);  subScene.setLayoutY(10);

		camera = new Camera(subScene);
		this.getChildren().add(subScene);

		ground = new Box(AXIS_LENGTH,0,AXIS_LENGTH);
		ground.setMaterial(groundMaterial);

		task = new Timeline(new KeyFrame(Duration.millis(40), ae -> {
				vehicle.updateState(model);
//				camera.updateState(model);
		} ) );
		task.setCycleCount(Timeline.INDEFINITE);
		task.play();


	}


	public MAV3DViewTab setup(ChartControlWidget recordControl, IMAVController control) {
		this.model = control.getCurrentModel();
		this.map   = new MapGroup(model);

		StateProperties.getInstance().getLandedProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				ground.setTranslateY(model.hud.al*100);
				camera.setTranslateY(model.hud.al*100);
			}
		});

		AmbientLight ambient = new AmbientLight();
		ambient.setColor(Color.WHITE);

		vehicle = new VehicleModel(50);
		world.getChildren().addAll(ground, map, vehicle, ambient);

		return this;
	}


	@Override
	public FloatProperty getScrollProperty() {
		return null;
	}


	@Override
	public void refreshChart() {
		map.clear();
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
}
