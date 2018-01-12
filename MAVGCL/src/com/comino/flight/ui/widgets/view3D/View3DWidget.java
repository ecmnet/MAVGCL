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

package com.comino.flight.ui.widgets.view3D;

import com.comino.flight.observables.StateProperties;
import com.comino.flight.ui.widgets.panel.ChartControlWidget;
import com.comino.flight.ui.widgets.view3D.objects.Camera;
import com.comino.flight.ui.widgets.view3D.objects.MapGroup;
import com.comino.flight.ui.widgets.view3D.objects.VehicleModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class View3DWidget extends SubScene  {


	private static final double PLANE_LENGTH = 2000.0;

	private Timeline 		task 		= null;
	private Xform 			world 		= new Xform();

	private Box             	ground     	= null;
	private Box             	northpole   = null;

	private MapGroup 		map			= null;
	private Camera 			camera		= null;
	private VehicleModel   	vehicle    	= null;

	private int				perspective = Camera.OBSERVER_PERSPECTIVE;


	public View3DWidget(Group root, double width, double height, boolean depthBuffer, SceneAntialiasing antiAliasing) {
		super(root, width, height, depthBuffer, antiAliasing);

		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);

		AmbientLight ambient = new AmbientLight();
		ambient.setColor(Color.WHITE);

		PhongMaterial groundMaterial = new PhongMaterial();
		groundMaterial.setDiffuseColor(Color.LIGHTGRAY);

		PhongMaterial northMaterial = new PhongMaterial();
		northMaterial.setDiffuseColor(Color.RED);

		northpole = new Box(1,100,1);
		northpole.setMaterial(northMaterial);
		northpole.setTranslateZ(PLANE_LENGTH/2.0f);

		ground = new Box(PLANE_LENGTH,0,PLANE_LENGTH);
		ground.setMaterial(groundMaterial);

		Text north = new Text("N");
		north.setTranslateZ(PLANE_LENGTH/2.0f);
		north.setTranslateY(60);
		north.setTranslateX(-4);
		north.setRotate(180);

		vehicle = new VehicleModel(50);
		world.getChildren().addAll(ground, northpole, vehicle, ambient, north);

		camera = new Camera(this);

	}

	public View3DWidget setup(ChartControlWidget recordControl, IMAVController control) {
		DataModel model = control.getCurrentModel();

		this.map   = new MapGroup(model);
		world.getChildren().addAll(map);

		StateProperties.getInstance().getLandedProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				camera.setTranslateY(model.hud.al*100);
				world.setTranslateY(model.hud.al*100);
			}
		});

		task = new Timeline(new KeyFrame(Duration.millis(40), ae -> {
			switch(perspective) {
			case Camera.OBSERVER_PERSPECTIVE:
				vehicle.updateState(model);
				break;
			case Camera.VEHICLE_PERSPECTIVE:
				camera.updateState(model);
				break;
			}
		} ) );
		task.setCycleCount(Timeline.INDEFINITE);
		task.play();

		return this;
	}

	public void setPerspective(int perspective) {
		this.perspective = perspective;
		camera.setPerspective(perspective);
		switch(perspective) {
		case Camera.OBSERVER_PERSPECTIVE:
			vehicle.setVisible(true);
			break;
		case Camera.VEHICLE_PERSPECTIVE:
			vehicle.setVisible(false);
			break;
		}
	}

	public void scale(float scale) {
		world.setScale(scale);
	}


	public void clear() {
		map.clear();
	}

}
