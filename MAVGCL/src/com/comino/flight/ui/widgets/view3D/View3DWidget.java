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
import javafx.util.Duration;

public class View3DWidget extends SubScene  {

	private static final double PLANE_LENGTH = 5000.0;

	private Timeline 		task 		= null;
	private Xform 			world 		= new Xform();

	private Box             	ground     	= null;
	private MapGroup 		map			= null;
	private Camera 			camera		= null;
	private VehicleModel   	vehicle    	= null;



	public View3DWidget(Group root, double width, double height, boolean depthBuffer, SceneAntialiasing antiAliasing) {
		super(root, width, height, depthBuffer, antiAliasing);

		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);

		AmbientLight ambient = new AmbientLight();
		ambient.setColor(Color.WHITE);

		PhongMaterial groundMaterial = new PhongMaterial();
		groundMaterial.setDiffuseColor(Color.LIGHTGRAY);

		ground = new Box(PLANE_LENGTH,0,PLANE_LENGTH);
		ground.setMaterial(groundMaterial);

		vehicle = new VehicleModel(50);
		world.getChildren().addAll(ground, vehicle, ambient);

		camera = new Camera(this);

	}

	public View3DWidget setup(ChartControlWidget recordControl, IMAVController control) {
		DataModel model = control.getCurrentModel();

		this.map   = new MapGroup(model);
		world.getChildren().addAll(map);

		StateProperties.getInstance().getLandedProperty().addListener((v,o,n) -> {
			if(n.booleanValue()) {
				ground.setTranslateY(model.hud.al*100);
				camera.setTranslateY(model.hud.al*100);
			}
		});

		task = new Timeline(new KeyFrame(Duration.millis(40), ae -> {
			vehicle.updateState(model);
			//			camera.updateState(model);
		} ) );
		task.setCycleCount(Timeline.INDEFINITE);
		task.play();


		return this;
	}

	public void clear() {
	map.clear();
	}

}
