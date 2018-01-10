package com.comino.flight.ui.widgets.view3D.objects;

import java.util.HashMap;
import java.util.Map;

import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.msp.model.DataModel;

import georegression.struct.point.Point3D_F32;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.util.Duration;

public class MapGroup extends Xform {

	private final Map<Integer,Box> blocks   	= new HashMap<Integer,Box>();

	private final PhongMaterial mapMaterial	= new PhongMaterial();

	private DataModel 		model		= null;
	private Timeline 		maptimer 	= null;

	public MapGroup(DataModel model) {

		super();

		this.model = model;

		mapMaterial.setDiffuseColor(Color.BLUE);
		mapMaterial.setSpecularColor(Color.LIGHTBLUE);

		maptimer = new Timeline(new KeyFrame(Duration.millis(250), ae -> {
				for(int k=0;k<this.getChildren().size();k++)
					this.getChildren().get(k).setVisible(false);
				model.grid.getData().forEach((i,b) -> {
					getBlockBox(i,b).setVisible(true);;
				});
		} ) );
		maptimer.setCycleCount(Timeline.INDEFINITE);
		maptimer.play();

	}

	public void clear() {
		blocks.forEach((i,p) -> {
			Platform.runLater(() -> {
				this.getChildren().remove(p);
			});
		});
		blocks.clear();
	}

	private Box getBlockBox(int block, Point3D_F32 b) {

		if(blocks.containsKey(block))
			return blocks.get(block);

		final Box box = new Box(5, 5, 5);

		box.setTranslateX(-b.y*100);
		box.setTranslateY(-b.z*100+box.getHeight()/2);
		box.setTranslateZ(b.x*100);
		box.setMaterial(mapMaterial);

	    this.getChildren().addAll(box);
		blocks.put(block,box);

		return box;
	}
}
