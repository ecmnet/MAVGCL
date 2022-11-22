package com.comino.flight.ui.widgets.view3D.objects;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

public class Obstacle extends Xform {
	
	private final Box pile = new Box(2,300,2);
	
	public Obstacle() {
		super();
		
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(Color.BLUE);
		
		pile.setMaterial(material);
		this.getChildren().addAll(pile);
		
	}
	
	public void updateState(AnalysisDataModel model) {
		if(!Double.isNaN(model.getValue("SLAMOBY"))) {
			pile.setVisible(true);
			this.setTranslate(-model.getValue("SLAMOBY")*100f, 0, model.getValue("SLAMOBX")*100f);
		} else
			pile.setVisible(false);
	}

}
