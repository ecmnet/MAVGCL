package com.comino.flight.ui.widgets.view3D.objects;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;

import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;

public class Obstacle extends Xform {
	
	private final Sphere        obstacle = new Sphere(20);
	private final Sphere        boundary = new Sphere(50);
	private final VehicleModel  vehicle;
	private final PhongMaterial boundary_no_collision = new PhongMaterial();
	private final PhongMaterial boundary_collision    = new PhongMaterial();
	private final Point3D_F64   o = new Point3D_F64();
	private final Point3D_F64   v = new Point3D_F64();
	
	public Obstacle(VehicleModel vehicle) {
		super();
		
		this.vehicle = vehicle;
		
		PhongMaterial material1 = new PhongMaterial();
		material1.setDiffuseColor(Color.web("rgba( 190,190,190,1.0 )"));
		obstacle.setMaterial(material1);
		
		boundary_no_collision.setDiffuseColor(Color.web("rgba( 10,10,10,0.1 )"));
		boundary_collision.setDiffuseColor   (Color.web("rgba( 50,10,10,0.1 )"));
		boundary.setMaterial(boundary_no_collision);
		
		this.getChildren().addAll(obstacle,boundary);
		
	}
	
	public void updateState(AnalysisDataModel model, float offset) {
		if(!Double.isNaN(model.getValue("SLAMOBY"))) {
			
			this.setVisible(true);
			o.setTo(-model.getValue("SLAMOBY")*100f, (-model.getValue("SLAMOBZ")-offset ) * 100f - 12, model.getValue("SLAMOBX")*100f);
			
			this.setTranslate(o.x,o.y,o.z);
			v.setTo(vehicle.getTranslateX(),vehicle.getTranslateY(),vehicle.getTranslateZ()); v.scale(-1f);
			v.plusIP(o);
			
			if(v.norm() < boundary.getRadius())
				boundary.setMaterial(boundary_collision);
			else
				boundary.setMaterial(boundary_no_collision);
		} else
			this.setVisible(false);
	}
	

}
