package com.comino.mav3d;


import com.interactivemesh.jfx.importer.obj.ObjModelImporter;

import javafx.scene.shape.MeshView;

public class VehicleModel extends Xform{

	ObjModelImporter obj = null;

	public VehicleModel() {
		super();
		obj = new ObjModelImporter();
		obj.read(this.getClass().getResource("res/quad_x.obj"));
		this.getChildren().addAll(obj.getImport());
		this.setScale(40);
		this.setRotateX(-90);
	}
}
