package com.comino.flight.ui.widgets.view3D.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.mavcom.model.DataModel;

import georegression.struct.point.Point3D_F32;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.util.Duration;

public class Map3DGroup {

	private final Group root;
	
	private double size   = 10;
	private double size2 = size / 2;
	private float resolution_m = 0.1f;
	private int   dimension    = 500;
	private int   dimension2   = dimension * dimension; 

	private Timeline         			maptimer 	= null;
	private final  Map<Integer,Box>    	boxes   	= new HashMap<Integer,Box>();
	private final List<PhongMaterial>  	blocked 	= new ArrayList<PhongMaterial>();
	
	private final Point3D_F32 tmp = new Point3D_F32();

	public Map3DGroup(Group root, DataModel model) {
		this.root = root;
		
		for(int i = 0; i < 20; i++) {
			PhongMaterial m = new PhongMaterial();
			m.setDiffuseColor(Color.web("DARKORANGE", (20 - i)/20f));
			blocked.add(m);
		}

//		maptimer = new Timeline(new KeyFrame(Duration.millis(50), ae -> {
//		  model.grid.getData().forEach((i,p) -> { handleBox(i); });	
//		})	);
//		maptimer.setCycleCount(Timeline.INDEFINITE);

		root.disabledProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
				maptimer.play();
			} else {
				maptimer.stop();
			}
		});
	}
	
	public void clear() {
		Platform.runLater(() -> {
		boxes.forEach((i,p) -> { root.getChildren().remove(p); });
		boxes.clear();
		});
	}

	private void handleBox(int block) {

		if(block > 0) {
			if(boxes.containsKey(block)) 
				return;
			
			Point3D_F32 pos = decode(block, tmp);
			
			final Box box = new Box(size, size, size);
			box.setTranslateZ(pos.x*size+size2);
			box.setTranslateX(pos.y*size+size2);
		//	box.setTranslateY(pos.z*size+size2);
			
			box.setMaterial(blocked.get((int)(pos.z/100)));
			box.setCullFace(CullFace.BACK);
			root.getChildren().add(box);
			boxes.put(block, box);


		} else {
			 System.out.println(block);	
			if(boxes.containsKey(-block))
				root.getChildren().remove(boxes.remove(-block));			
		}	
	}
	
	
	private Point3D_F32 decode(long block, Point3D_F32 pos) {
		
		pos.x = block % dimension * resolution_m;
		pos.y = ( block / dimension) % dimension *resolution_m;
		pos.z = ( block / dimension2 )*resolution_m;
		
		return pos;
	}



}
