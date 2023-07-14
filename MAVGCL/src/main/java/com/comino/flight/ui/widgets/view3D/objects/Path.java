package com.comino.flight.ui.widgets.view3D.objects;

import java.util.ArrayList;
import java.util.List;

import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.composites.PolyLine3D;
import org.fxyz3d.shapes.composites.PolyLine3D.LineType;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;

import javafx.scene.DepthTest;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;

public class Path extends Xform {


	private final List<Point3D> points = new ArrayList<Point3D>();
	private final Sphere        sphere = new Sphere(1.3);
	
	private PolyLine3D line;

	private boolean enabled = true;

	public Path() {
		super();
		this.setDepthTest(DepthTest.ENABLE);
		line = new PolyLine3D(points, 1, Color.DARKBLUE, LineType.TRIANGLE);
		
		final PhongMaterial line_material = new PhongMaterial();
		line_material.setDiffuseColor(Color.DARKBLUE);
		sphere.setMaterial(line_material);
		
	}
	

	@SuppressWarnings("deprecation")
	public void updateState(AnalysisDataModel model, double offset) {

		if(model==null || !enabled)
			return;

		

			if(!points.isEmpty()) {
				line = new PolyLine3D(points, 1, Color.DARKRED, LineType.TRIANGLE);
				// Endpoint dot
//				sphere.setTranslateX(-y*100);
//				sphere.setTranslateY(-(z+offset)*100+Z_OFFSET);
//				sphere.setTranslateZ(x*100);
//				sphere.setVisible(true);
//				this.getChildren().clear();
//				this.getChildren().addAll(line, sphere);
			}
			
	
				
	}

	public void clear() {
		this.getChildren().clear();
	}


	public void show(boolean show) {
		this.enabled = show;
		if(!show)
			this.getChildren().clear();
	}


}
