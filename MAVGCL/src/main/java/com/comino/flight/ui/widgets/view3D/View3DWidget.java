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

import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.flight.ui.widgets.view3D.objects.Camera;
import com.comino.flight.ui.widgets.view3D.objects.Map3DGroup;
import com.comino.flight.ui.widgets.view3D.objects.Target;
import com.comino.flight.ui.widgets.view3D.objects.VehicleModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.mavcom.control.IMAVController;

import georegression.struct.point.Point3D_F64;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Text;

public class View3DWidget extends SubScene implements IChartControl {


	private static final double PLANE_LENGTH = 2000.0;
	

	private AnimationTimer 	task 		= null;
	private Xform 			world 		= new Xform();

	private Box             ground;

//	private MapGroup 		blocks		= null;
	private Map3DGroup      blocks      = null;
	private Camera 			camera		= null;
	private VehicleModel   	vehicle    	= null;
	private Target			target      = null;

	private FloatProperty   scroll       = new SimpleFloatProperty(0);
	private FloatProperty   replay       = new SimpleFloatProperty(0);

	private AnalysisDataModel model      = null;
	private StateProperties state        = null;
	
	private float           offset       = 0;

	private int				perspective = Camera.OBSERVER_PERSPECTIVE;

	private AnalysisModelService  dataService = AnalysisModelService.getInstance();


	public View3DWidget(Group root, double width, double height, boolean depthBuffer, SceneAntialiasing antiAliasing) {
		super(root, width, height, depthBuffer, antiAliasing);

		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);

		this.state = StateProperties.getInstance();

		AmbientLight ambient = new AmbientLight();
		ambient.setColor(Color.web("DARKGRAY", 0.1));
		

		PhongMaterial groundMaterial = new PhongMaterial();
		//	groundMaterial.setDiffuseColor(Color.LIGHTGRAY);
		groundMaterial.setDiffuseMap(new Image
				(getClass().getResource("objects/resources/ground.jpg").toExternalForm()));
		
		PointLight pointLight = new PointLight(Color.web("GRAY", 0.5));
	    pointLight.setTranslateX(100);
	    pointLight.setTranslateY(800);
	    pointLight.setRotate(45);
	    pointLight.setTranslateZ(-800);
	   


		PhongMaterial northMaterial = new PhongMaterial();
		northMaterial.setDiffuseColor(Color.RED);

		target    = new Target();

		ground = new Box(PLANE_LENGTH,0,PLANE_LENGTH);
		ground.setMaterial(groundMaterial);

		vehicle = new VehicleModel(75);
		world.getChildren().addAll(ground, vehicle,  target, pointLight, ambient,
				addPole('N'), addPole('S'),addPole('W'),addPole('E'));

		camera = new Camera(this);

	}

	public View3DWidget setup(IMAVController control) {

		this.model = dataService.getCurrent();

		this.blocks   = new Map3DGroup(world,control.getCurrentModel());
		
//		this.map   = new Map3DGroup(world,control.getCurrentModel());
		

		state.getLandedProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				if(!Double.isNaN(model.getValue("ALTTR"))) {
					camera.setTranslateY(model.getValue("ALTTR")*100);
					world.setTranslateY(model.getValue("ALTTR")*100);
					this.offset = -(float)model.getValue("LPOSZ");
				}
			}
		});


		state.getReplayingProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				if(!Double.isNaN(model.getValue("ALTTR"))) {
					camera.setTranslateY(model.getValue("ALTTR")*100);
					world.setTranslateY(model.getValue("ALTTR")*100);
				}
			}
		});

		state.getConnectedProperty().addListener((v,o,n) -> {
			vehicle.setVisible(n.booleanValue());
		});

		state.getArmedProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				this.model = dataService.getCurrent();
			}
		});
		
		
		task = new AnimationTimer() {
			@Override
			public void handle(long now) {
				target.updateState(model);
				vehicle.updateState(model,offset);
				switch(perspective) {
				case Camera.OBSERVER_PERSPECTIVE:
					vehicle.updateState(model,offset);
					vehicle.setVisible(true);
					break;
				case Camera.VEHICLE_PERSPECTIVE:
					camera.updateState(model);
					break;
				}
			}		
		};



		scroll.addListener((v, ov, nv) -> {
			if(StateProperties.getInstance().getRecordingProperty().get()==AnalysisModelService.STOPPED) {
				int current_x1_pt = dataService.calculateIndexByFactor(nv.floatValue());

				if(dataService.getModelList().size()>0 && current_x1_pt >= 0 && current_x1_pt< dataService.getModelList().size())
					model = dataService.getModelList().get(current_x1_pt);
				else
					model = dataService.getCurrent();

			}
		});


		replay.addListener((v, ov, nv) -> {
			if(nv.intValue()<=5) {
				model = dataService.getModelList().get(1);
			} else
				model = dataService.getModelList().get(nv.intValue());
		});


		this.visibleProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
				task.start();
			} else {
				task.stop();
			}
		});

		this.disabledProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
				task.start();
			} else {
				task.stop();
			}
		});

		return this;
	}
	

	public void setPerspective(int perspective) {
		this.perspective = perspective;
		Platform.runLater(() -> {
			camera.setPerspective(perspective,model);
			switch(perspective) {
			case Camera.OBSERVER_PERSPECTIVE:
				vehicle.show(true);
//				map.setMode2D(false);
				break;
			case Camera.VEHICLE_PERSPECTIVE:
				vehicle.show(false);
//				map.setMode2D(true);
				break;
			}
		});
	}

	public void scale(float scale) {
		Platform.runLater(() -> {
			switch(perspective) {
			case Camera.OBSERVER_PERSPECTIVE:
				world.setScale(scale/100);
				break;
			case Camera.VEHICLE_PERSPECTIVE:
				camera.setFieldOfView(scale);
				break;
			}
		});
	}


//	public void clear() {
//		blocks.clear();
//	}

	private Group addPole(char orientation) {

		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(Color.RED);

		Xform pole = new Xform();
		Box pile = new Box(1,100,1);
		pile.setMaterial(material);
		Text text = new Text(String.valueOf(orientation));
		text.setRotate(180);
		text.setTranslateY(60);

		switch(orientation) {
		case 'N':
			text.setTranslateX(-4);
			pole.setTranslateZ(PLANE_LENGTH/2.0f);
			break;
		case 'S':
			text.setTranslateX(-3);
			pole.setRotateY(180);
			pole.setTranslateZ(-PLANE_LENGTH/2.0f);
			break;
		case 'E':
			text.setTranslateX(-3);
			pole.setRotateY(270);
			pole.setTranslateX(-PLANE_LENGTH/2.0f);
			break;
		case 'W':
			text.setTranslateX(-5);
			pole.setRotateY(90);
			pole.setTranslateX(PLANE_LENGTH/2.0f);
			break;
		}

		pole.getChildren().addAll(pile,text);
		return pole;
	}

	@Override
	public IntegerProperty getTimeFrameProperty() {
		return null;
	}

	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}

	@Override
	public FloatProperty getReplayProperty() {
		return replay;
	}

	@Override
	public BooleanProperty getIsScrollingProperty() {
		return null;
	}

	@Override
	public void refreshChart() {
	}

	@Override
	public KeyFigurePreset getKeyFigureSelection() {
		return null;
	}

	@Override
	public void setKeyFigureSelection(KeyFigurePreset preset) {
	}
	
	
}
