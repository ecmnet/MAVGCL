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

public class Trajectory extends Xform {

	private static final float STEP     = 0.2f;
	private static final float Z_OFFSET = 18f;

	private final List<Point3D> points = new ArrayList<Point3D>();
	private final Sphere sphere = new Sphere(1.3);
	private PolyLine3D line;

	private float  p0x = 0;
	private float  v0x = 0;
	private float  a0x = 0;

	private float  p0y = 0;
	private float  v0y = 0;
	private float  a0y = 0;

	private float  p0z = 0;
	private float  v0z = 0;
	private float  a0z = 0;

	private float x= 0; 
	private float y= 0;
	private float z= 0;

	private boolean enabled = true;

	public Trajectory() {
		super();
		this.setDepthTest(DepthTest.ENABLE);
		line = new PolyLine3D(points, 1, Color.DARKRED, LineType.TRIANGLE);
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(Color.DARKRED);
		sphere.setMaterial(material);
	}

	@SuppressWarnings("deprecation")
	public void updateState(AnalysisDataModel model, double offset) {

		if(model==null || !enabled)
			return;

		double current = model.getValue("TRAJCURRENT");
		double length  = model.getValue("TRAJLEN");

		if(!Double.isNaN(current) && !Double.isNaN(length) && current >= 0 && length >0 ) {

			points.clear();

			p0x = (float)model.getValue("TRAJSTARTX");
			p0y = (float)model.getValue("TRAJSTARTY");
			p0z = (float)model.getValue("TRAJSTARTZ");
			v0x = (float)model.getValue("TRAJSTARTVX");
			v0y = (float)model.getValue("TRAJSTARTVY");
			v0z = (float)model.getValue("TRAJSTARTVZ");
			a0x = (float)model.getValue("TRAJSTARTAX");
			a0y = (float)model.getValue("TRAJSTARTAY");
			a0z = (float)model.getValue("TRAJSTARTAZ");

			for(double t = current+STEP; t < length; t = t + STEP ) {

				x = getPosition((float)t, p0x, v0x, a0x,(float)model.getValue("TRAJALPHAX"),(float)model.getValue("TRAJBETAX"),(float)model.getValue("TRAJGAMMAX"));
				y = getPosition((float)t, p0y, v0y, a0y,(float)model.getValue("TRAJALPHAY"),(float)model.getValue("TRAJBETAY"),(float)model.getValue("TRAJGAMMAY"));
				z = getPosition((float)t, p0z, v0z, a0z,(float)model.getValue("TRAJALPHAZ"),(float)model.getValue("TRAJBETAZ"),(float)model.getValue("TRAJGAMMAZ"));

				points.add(new Point3D(-y*100,-(z+offset)*100+Z_OFFSET,x*100));

			}
			
			x = getPosition((float)length, p0x, v0x, a0x,(float)model.getValue("TRAJALPHAX"),(float)model.getValue("TRAJBETAX"),(float)model.getValue("TRAJGAMMAX"));
			y = getPosition((float)length, p0y, v0y, a0y,(float)model.getValue("TRAJALPHAY"),(float)model.getValue("TRAJBETAY"),(float)model.getValue("TRAJGAMMAY"));
			z = getPosition((float)length, p0z, v0z, a0z,(float)model.getValue("TRAJALPHAZ"),(float)model.getValue("TRAJBETAZ"),(float)model.getValue("TRAJGAMMAZ"));

			points.add(new Point3D(-y*100,-(z+offset)*100+Z_OFFSET,x*100));

//			// Endpoint dot
//			sphere.setTranslateX(-y*100);
//			sphere.setTranslateY(-(z+offset)*100+Z_OFFSET);
//			sphere.setTranslateZ(x*100);
//			sphere.setVisible(true);

			if(!points.isEmpty()) {
				line = new PolyLine3D(points, 1, Color.DARKRED, LineType.TRIANGLE);
				// Endpoint dot
				sphere.setTranslateX(-y*100);
				sphere.setTranslateY(-(z+offset)*100+Z_OFFSET);
				sphere.setTranslateZ(x*100);
				sphere.setVisible(true);
				this.getChildren().clear();
				this.getChildren().addAll(line, sphere);
			}
		} 
		else 
			clear();
	}

	public void clear() {
		this.getChildren().clear();
	}


	public void show(boolean show) {
		this.enabled = show;
		if(!show)
			this.getChildren().clear();
	}



	private float getPosition(float t, float p0, float v0, float a0, float a, float b, float g) { 
		return p0 + v0*t + (1.0f/2.0f)*a0*t*t + (1.0f/6.0f)*g*t*t*t + (1.0f/24.0f)*b*t*t*t*t + (1.0f/120.0f)*a*t*t*t*t*t; 
	}

}
