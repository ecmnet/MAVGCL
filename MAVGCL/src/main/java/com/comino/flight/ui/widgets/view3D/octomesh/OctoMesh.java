package com.comino.flight.ui.widgets.view3D.octomesh;

import java.util.HashMap;
import java.util.Map;

import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.MAVScatterMesh;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;

public class OctoMesh extends MeshView {
	
	private static final int MESHCOUNT = 25;
	
	private static Color color = Color.CORNFLOWERBLUE; //
	private final static double MIN = 0 ;
	private final static double MAX = 1000 ;
	private final static double BLUE_HUE = Color.BLUE.getHue() ;
	private final static double RED_HUE = Color.RED.getHue() ;

	private Map<Long,Point3D> cubes = new HashMap<>(MESHCOUNT);

	private MAVScatterMesh    mesh;

	private Group root;

	
	public OctoMesh(Group root,int size) {
		this.root   = root;
		mesh = new MAVScatterMesh(cubes.values(),size);
		root.getChildren().add(mesh);
		
	}

	public boolean hasSpace() {
		return cubes.size()<MESHCOUNT;
	}

	public void add(long id, float x, float y, float z) {
		cubes.put(id,new Point3D(x,y+10,z,0));
		mesh.update();
		mesh.setTextureModeNone(color);
	}

	public boolean remove(long id) {
		cubes.remove(id);
		if(cubes.isEmpty()) {
			mesh.update();
			root.getChildren().remove(mesh);
			return true;
		}
		mesh.update();
		mesh.setTextureModeNone(color);
		return false;
	}
	
	public String toString() {
		return "size: "+cubes.size();
	}
	
	private Color getColorForValue(double value) {
		if (value < MIN || value > MAX) {
			return Color.BLACK ;
		}
		double hue = BLUE_HUE + (RED_HUE - BLUE_HUE) * (value - MIN) / (MAX - MIN) ;
		return Color.hsb(hue, 1.0, 1.0);
	}
	
	

}
