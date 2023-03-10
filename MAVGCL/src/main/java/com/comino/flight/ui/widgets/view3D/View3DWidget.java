/****************************************************************************
 *
 *   Copyright (c) 2017,2023 Eike Mansfeld ecm@gmx.de.
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
import com.comino.flight.ui.widgets.view3D.objects.Map3DOctoGroup;
import com.comino.flight.ui.widgets.view3D.objects.Obstacle;
import com.comino.flight.ui.widgets.view3D.objects.Target;
import com.comino.flight.ui.widgets.view3D.objects.Trajectory;
import com.comino.flight.ui.widgets.view3D.objects.VehicleModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.Vision;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

public class View3DWidget extends SubScene implements IChartControl {


	private static final double PLANE_LENGTH  = 5000.0;
	private static final float  VEHICLE_SCALE = 15.0f;


	private AnimationTimer 	task 		= null;
	private Xform 			world 		= new Xform();

	private Group           ground;
	private Box             landing_target;
	private Rotate          rf = new Rotate(0, Rotate.Y_AXIS);

	//	private MapGroup 		blocks		= null;
	private Map3DOctoGroup      blocks      = null;
	private Camera 			camera		= null;
	private VehicleModel   	vehicle    	= null;
	private Trajectory   	trajectory  = null;
	private Target			target      = null;
	private Obstacle		obstacle      = null;

	private FloatProperty   scroll       = new SimpleFloatProperty(0);
	private FloatProperty   replay       = new SimpleFloatProperty(0);

	private AnalysisDataModel model      = null;
	private AnalysisDataModel takeoff    = null;
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
	
		PointLight pointLight = new PointLight(Color.web("GRAY", 0.5));
		pointLight.setTranslateX(100);
		pointLight.setTranslateY(800);
		pointLight.setRotate(45);
		pointLight.setTranslateZ(-800);

		PhongMaterial northMaterial = new PhongMaterial();
		northMaterial.setDiffuseColor(Color.RED);

		target    = new Target();

		ground = createGround();
		
		landing_target = new Box(40,3,40);
		PhongMaterial landing_target_material = new PhongMaterial();
		landing_target_material.setDiffuseMap(new Image(this.getClass().getResourceAsStream("fiducial.png")));
		landing_target.setMaterial(landing_target_material);

		vehicle   = new VehicleModel(VEHICLE_SCALE);
		obstacle  = new Obstacle(vehicle);
		trajectory = new Trajectory();
		world.getChildren().addAll(ground,landing_target, target, trajectory, obstacle, vehicle, pointLight, ambient,
				addPole('N'), addPole('S'),addPole('W'),addPole('E'));

		camera = new Camera(this);
		trajectory.show(true);
		
		setPerspective(Camera.OBSERVER_PERSPECTIVE);

	}

	public View3DWidget setup(IMAVController control) {

		this.model = dataService.getCurrent();

		this.blocks   = new Map3DOctoGroup(world,control);

		state.getLandedProperty().addListener((v,o,n) -> {
			if(n.booleanValue() && !state.getLogLoadedProperty().get()) {
				if(!Double.isNaN(model.getValue("ALTTR"))) {
					camera.setTranslateY(model.getValue("ALTTR")*100);
					world.setTranslateY(model.getValue("ALTTR")*100);
					if(Double.isFinite(model.getValue("LPOSRZ")) && ( model.getValue("LPOSRZ") != 0.0))
						offset = -(float)model.getValue("LPOSRZ");
					else
						offset = -(float)model.getValue("LPOSZ");
				}
			}
		});


		// search for takeoff 
		// TODO: What if no takeoff found
		//
		state.getLogLoadedProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				takeoff = findTakeOff();
				model = dataService.getModelList().get(dataService.getModelList().size()-1);
			} 
			else
				model = dataService.getCurrent();
		});

		state.getRecordingAvailableProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				takeoff = findTakeOff();
				model = dataService.getModelList().get(dataService.getModelList().size()-1);
			} 
			else
				model = dataService.getCurrent();
		});


		// search for takeoff
		state.getReplayingProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				takeoff = findTakeOff();
			}
		});

//		state.getConnectedProperty().addListener((v,o,n) -> {
//			vehicle.setVisible(n.booleanValue());
//		});

		state.getArmedProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				model = dataService.getCurrent();
			}
		});


		task = new AnimationTimer() {
			
			private long tms_old=0;
			
			@Override
			public void handle(long now) {
				
				if((now - tms_old) < 20_000_000)
					return;
				
				tms_old = now;
				
				if(isDisabled())
					return;
				
				if(takeoff!=null && state.getLogLoadedProperty().get() || state.getReplayingProperty().get() || state.getRecordingAvailableProperty().get()) {

					if(!Double.isNaN(takeoff.getValue("ALTTR"))) {
						camera.setTranslateY(takeoff.getValue("ALTTR")*100);
						world.setTranslateY(takeoff.getValue("ALTTR")*100);
					} 
					if(Double.isFinite(model.getValue("LPOSRZ")))
						offset = -(float)takeoff.getValue("LPOSRZ");
					else
						offset = -(float)takeoff.getValue("LPOSZ");


				} else {

					if(state.getLandedProperty().get()) {

						if(!Double.isNaN(model.getValue("ALTTR"))) {
							camera.setTranslateY(model.getValue("ALTTR")*100);
							world.setTranslateY(model.getValue("ALTTR")*100);
						} 
						if(Double.isFinite(model.getValue("LPOSRZ")))
							offset = -(float)model.getValue("LPOSRZ");
						else
							offset = -(float)model.getValue("LPOSZ");
					} 
				}
				
				if((((int)model.getValue("VISIONFLAGS")) & 1 << Vision.FIDUCIAL_LOCKED ) == 1 << Vision.FIDUCIAL_LOCKED) {
					
					landing_target.getTransforms().clear();
					landing_target.setTranslateX(-model.getValue("PRECLOCKY")*100f);
					landing_target.setTranslateZ(model.getValue("PRECLOCKX")*100f);		
					addRotate(landing_target, rf,180 - model.getValue("PRECLOCKW"));
					landing_target.setVisible(true);
					
				} else
					landing_target.setVisible(false);

				
				switch(perspective) {
				case Camera.OBSERVER_PERSPECTIVE:
					if(!vehicle.isVisible())
						vehicle.setVisible(true);
					vehicle.updateState(model,offset);
					trajectory.updateState(model,offset);
					break;
				case Camera.BIRDS_PERSPECTIVE:
					if(!vehicle.isVisible())
						vehicle.setVisible(true);
					vehicle.updateState(model,offset);
					trajectory.updateState(model,offset);
					camera.updateState(model);
					break;
				case Camera.VEHICLE_PERSPECTIVE:
					camera.updateState(model);
					trajectory.clear();
					break;
				}
				
				obstacle.updateState(model,offset);
			}		
		};

		scroll.addListener((v, ov, nv) -> {
			if(StateProperties.getInstance().getRecordingProperty().get()==AnalysisModelService.STOPPED) {
				int current_x1_pt = dataService.calculateIndexByFactor(nv.floatValue());

				if(dataService.getModelList().size()>0 && current_x1_pt >= 0 && current_x1_pt< dataService.getModelList().size())
					model = dataService.getModelList().get(current_x1_pt);
				//				else
				//					model = dataService.getCurrent();

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
				trajectory.clear();
			}
		});


		this.disabledProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
				task.start();
			} else {
				task.stop();
				trajectory.clear();
			}
			blocks.enable(!n.booleanValue());
		});
		
		
        blocks.enable(true);
		return this;
	}

	public void enableTrajectoryView(boolean enabled) {
		trajectory.show(enabled);
	}
	
	public void enableObstacleView(boolean enabled) {
		obstacle.show(enabled);
	}
	
	public void setDataSource(int mode) {
		vehicle.setMode(mode);
	}


	public void setPerspective(int perspective) {
		this.perspective = perspective;
		Platform.runLater(() -> {
			camera.setPerspective(perspective,model);
			switch(perspective) {
			case Camera.BIRDS_PERSPECTIVE:
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
			case Camera.BIRDS_PERSPECTIVE:
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

	private AnalysisDataModel findTakeOff() {
		
		if(dataService.getModelList().size()<1)
			return dataService.getCurrent();

		AnalysisDataModel to = dataService.getModelList().get(0);
		for(int i = 0; i < dataService.getModelList().size();i++) {
			if(dataService.getModelList().get(i).msg!=null && dataService.getModelList().get(i).msg.text.contains("akeoff")) {
				to = dataService.getModelList().get(i);
				System.out.println("Takeoff found at "+i);
				break;
			}
		}
		return to;
	}
	
	private void addRotate(Box node, Rotate rotate, double angle) {

		Affine affine = node.getTransforms().isEmpty() ? new Affine() : (Affine)(node.getTransforms().get(0));

		double A11 = affine.getMxx(), A12 = affine.getMxy(), A13 = affine.getMxz();
		double A21 = affine.getMyx(), A22 = affine.getMyy(), A23 = affine.getMyz();
		double A31 = affine.getMzx(), A32 = affine.getMzy(), A33 = affine.getMzz();


		// rotations over local axis
		Rotate newRotateX = new Rotate(angle, new Point3D(A11, A21, A31));
		Rotate newRotateY = new Rotate(angle, new Point3D(A12, A22, A32));
		Rotate newRotateZ = new Rotate(angle, new Point3D(A13, A23, A33));


		// apply rotation
		affine.prepend(rotate.getAxis() == Rotate.X_AXIS ? newRotateX :
			rotate.getAxis() == Rotate.Y_AXIS ? newRotateY : newRotateZ);
		node.getTransforms().setAll(affine);
	}
	
	private Group createGround() {
		
		final int BOX_COUNT = 14;
		final String ground_image = "tiles.jpg";
		
		Group g = new Group();
		
		PhongMaterial groundMaterial = new PhongMaterial();
		groundMaterial.setDiffuseMap(new Image
				(getClass().getResource("objects/resources/"+ground_image).toExternalForm()));
		
		final double side = PLANE_LENGTH/BOX_COUNT;
		for(int x = -BOX_COUNT; x < BOX_COUNT; x++) {
			for(int y = -BOX_COUNT; y < BOX_COUNT; y++) {
				final Box ground = new Box(side,0,side);
				ground.setMaterial(groundMaterial);
				ground.setTranslateX(side*x);
				ground.setTranslateZ(side*y);
				g.getChildren().add(ground);
			}
		}
		
		return g;
		
	}




}
