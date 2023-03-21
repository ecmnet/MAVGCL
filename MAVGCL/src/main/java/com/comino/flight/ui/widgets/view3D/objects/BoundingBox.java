package com.comino.flight.ui.widgets.view3D.objects;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

public class BoundingBox extends Xform {
	
	private final Box  boundingBox = new Box(100,100,100);
	
	private float  p0x = 0;
	private float  p0y = 0;
	private float  p0z = 0;
	
	public BoundingBox() {
		
		final PhongMaterial box_material = new PhongMaterial();
		box_material.setDiffuseColor(Color.web("303030",0.1f));
		boundingBox.setMaterial(box_material);
		this.getChildren().add(boundingBox);
		boundingBox.setVisible(false);
		
	}
	
	public void updateState(AnalysisDataModel model, double offset) {
		
	
		float bbx = (float)model.getValue("SLAMBBX");
		float bby = (float)model.getValue("SLAMBBY");
		float bbz = (float)model.getValue("SLAMBBZ");
		
		p0x = (float)model.getValue("TRAJSTARTX");
		p0y = (float)model.getValue("TRAJSTARTY");
		p0z = (float)model.getValue("TRAJSTARTZ");
	
		
		if(bbx == 0 || bby == 0 || bbz == 0)  {
			boundingBox.setVisible(false);
			return;
		}
		
		boundingBox.setVisible(true);
	}
	
	private float getPosition(float t, float p0, float v0, float a0, float a, float b, float g) { 
		return p0 + v0*t + (1.0f/2.0f)*a0*t*t + (1.0f/6.0f)*g*t*t*t + (1.0f/24.0f)*b*t*t*t*t + (1.0f/120.0f)*a*t*t*t*t*t; 
	}

}
