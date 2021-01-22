package com.comino.flight.ui.widgets.view3D.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.flight.model.map.MAVGCLMap;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.Map3DSpacialInfo;

import bubo.maps.d3.grid.CellProbability_F64;
import georegression.struct.point.Point3D_F64;
import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;

public class Map3DGroup {

	private final Group root;
	
	private AnimationTimer              task    = null;
	private final List<PhongMaterial>  	blocked = new ArrayList<PhongMaterial>();
	private final Map<Long,Box>         boxes   = new HashMap<Long,Box>();
	
	private final MAVGCLMap             map;
	private final Map3DSpacialInfo      info;
	private double                      size = 0;
	
	private final Point3D_F64           global = new Point3D_F64();
	private long  tms = 0;
	

	public Map3DGroup(Group root, DataModel model) {
		
		this.root = root;
		this.map  = MAVGCLMap.getInstance();
		this.info = map.getInfo();
		this.size = info.getCellSize() * 100;
		
		for(int i = 0; i < 5; i++) {
			PhongMaterial m = new PhongMaterial();
			m.setDiffuseColor(Color.web("DARKCYAN", i/5f));
			m.setSpecularColor(Color.WHITE);
			blocked.add(m);
		}
		
		task = new AnimationTimer() {
			@Override
			public void handle(long now) {
				
				if(map.isEmpty()) {
				  root.getChildren().removeAll(boxes.values());
				  boxes.clear();
				  return;
				}
				map.getLatestMapItems(tms).forEachRemaining((p) -> { addBlock(p); });
				tms = System.currentTimeMillis();
			}
		};

		root.disabledProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
				task.start();
			} else {
				task.stop();
			}
		});
	}
	
	public void addBlock(CellProbability_F64 pos) {

		long h = info.encodeMapPoint(pos,0);

		if(pos.probability > 0.5) {

			if(boxes.containsKey(h)) {
				return;
			}

			info.mapToGlobal(pos, global);

			// TODO: Fix rotation
			final Box box = new Box(size, size, size);
			box.setTranslateZ(global.x*100);
			box.setTranslateX(-global.y*100);
			box.setTranslateY(-global.z*100);
			box.setMaterial(blocked.get((int)(pos.probability*5)-1));
			box.setCullFace(CullFace.BACK);
			root.getChildren().add(box);
			boxes.put(h, box);
			return;

		} 
		
		else {

	//	if(pos.probability < 0.1f || pos.probability == 0.5f) {
			Box box = boxes.remove(h);
			if(box!=null) {
				root.getChildren().remove(box);	
			} 
		}
	}
	
//	public void clear() {
//		root.getChildren().clear();
//		boxes.clear();
//		
//	}
	
	public void invalidate() {
		this.tms = 0;
	}

	

}
