package com.comino.flight.ui.widgets.view3D.objects;

import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.msp.model.DataModel;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class Target extends Xform {

	private TriangleMesh pyramidMesh = new TriangleMesh();

	public Target() {
		super();

		final float h = 6;                      // Height
		final float s = 10;                      // Side

		pyramidMesh.getTexCoords().addAll(0,0);
		pyramidMesh.getPoints().addAll(
				0,    h/2,     0,            // Point 0 - Top
				0,    -h/2,    -s/2,         // Point 1 - Front
				-s/2, -h/2,    0,            // Point 2 - Left
				s/2,  -h/2,    0,            // Point 3 - Back
				0,    -h/2,    s/2           // Point 4 - Right
				);

		pyramidMesh.getFaces().addAll(
				0,0,  2,0,  1,0,          // Front left face
				0,0,  1,0,  3,0,          // Front right face
				0,0,  3,0,  4,0,          // Back right face
				0,0,  4,0,  2,0,          // Back left face
				4,0,  1,0,  2,0,          // Bottom rear face
				4,0,  3,0,  1,0           // Bottom front face
				);

		MeshView pyramid = new MeshView(pyramidMesh);

		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(Color.DARKORCHID);
		pyramid.setMaterial(material);
		pyramid.setDrawMode(DrawMode.FILL);
		this.getChildren().addAll(pyramid);
	}

	public void updateState(DataModel model) {
		if(model.slam.pd != 0) {
			this.setVisible(true);
			this.setTranslate(-model.slam.px*100f, model.state.l_z > 0 ? 0 : -model.state.l_z *100f, model.slam.py*100f);
		} else
			this.setVisible(false);
	}

}
