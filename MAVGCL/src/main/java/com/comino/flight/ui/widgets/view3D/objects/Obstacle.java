package com.comino.flight.ui.widgets.view3D.objects;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;

public class Obstacle extends Xform {
	
	private final Sphere       obstacle = new Sphere(20);
	private final Sphere       boundary = new Sphere(50);
	private final VehicleModel vehicle;
	
	public Obstacle(VehicleModel vehicle) {
		super();
		
		this.vehicle = vehicle;
		
		PhongMaterial material1 = new PhongMaterial();
		material1.setDiffuseColor(Color.web("rgba( 190,190,190,1.0 )"));
		obstacle.setMaterial(material1);
		
		PhongMaterial material2 = new PhongMaterial();
		material2.setDiffuseColor(Color.web("rgba( 10,10,10,0.1 )"));
		boundary.setMaterial(material2);
		
		this.getChildren().addAll(obstacle,boundary);
		
	}
	
	public void updateState(AnalysisDataModel model) {
		if(!Double.isNaN(model.getValue("SLAMOBY"))) {
			
			// Todo Collision check
			
			this.setVisible(true);
			// TODO: Check if center moved
			this.setTranslate(-model.getValue("SLAMOBY")*100f, -model.getValue("SLAMOBZ")*100f, model.getValue("SLAMOBX")*100f);
		} else
			this.setVisible(false);
	}

}
