package com.comino.flight.ui.widgets.view3D.objects;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.utils.MSP3DUtils;

import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;

public class Obstacle extends Xform {
	
	private final Box           obstacle = new Box();
	private final VehicleModel  vehicle;
	private final PhongMaterial boundary_no_collision = new PhongMaterial();
	private final PhongMaterial boundary_collision    = new PhongMaterial();
	private final Point3D_F64   o = new Point3D_F64();
	private final Point3D_F64   s = new Point3D_F64();
	
	private boolean show = false;
	
	public Obstacle(VehicleModel vehicle) {
		super();
		
		this.vehicle = vehicle;
		
		boundary_no_collision.setDiffuseColor(Color.web("rgba( 50,50,50,0.9 )"));
		boundary_collision.setDiffuseColor   (Color.web("rgba( 90,10,10,0.7 )"));
		obstacle.setMaterial(boundary_no_collision);
		
		this.getChildren().addAll(obstacle);
		
	}
	
	public void updateState(AnalysisDataModel model, float offset) {
	
		o.setTo(-model.getValue("SLAMOBY")*100f, (-model.getValue("SLAMOBZ")-offset ) * 100f, model.getValue("SLAMOBX")*100f);
		s.setTo( model.getValue("SLAMSOBY")*100f, (model.getValue("SLAMSOBZ")) * 100f, model.getValue("SLAMSOBX")*100f);
		
		if(show && MSP3DUtils.isFinite(o) && o.normSq()>0) {
			
			this.setTranslate(o.x,o.y+10,o.z);
			this.obstacle.setWidth(s.x);
			this.obstacle.setHeight(s.y);
			this.obstacle.setDepth(s.z);
			
			
			if(this.getBoundsInParent().intersects(vehicle.getBoundsInParent())) {
				obstacle.setMaterial(boundary_collision);
			} else {
				obstacle.setMaterial(boundary_no_collision);
			}

			this.setVisible(true);
		} else
			this.setVisible(false);
	}
	
	public void show(boolean show) {
		this.show = show;
	}
	

}
