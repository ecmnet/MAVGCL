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
import georegression.struct.point.Point3D_I32;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;

public class Map3DGroup {

	private final Group root;

	private AnimationTimer              task    = null;
	private final List<PhongMaterial>  	blocked = new ArrayList<PhongMaterial>();
	private final Map<Long,Box>         boxes   = new HashMap<Long,Box>();

	private final PhongMaterial markerMaterial  = new PhongMaterial();

	private final MAVGCLMap             map;
	private final Map3DSpacialInfo      info;
	private double                      size = 0;

	private final Point3D_I32           local =  new Point3D_I32();
	private final Point3D_F64           global = new Point3D_F64();
	private long  tms = 0;

	private Marker currentMarker = new Marker();


	public Map3DGroup(Group root, DataModel model) {

		this.root = root;
		this.map  = MAVGCLMap.getInstance();
		this.info = map.getInfo();
		this.size = info.getCellSize() * 100;

		for(int i = 0; i < 5; i++) {
			PhongMaterial m = new PhongMaterial();
			m.setDiffuseColor(Color.web("DARKCYAN", 1));
			m.setSpecularColor(Color.WHITE);
			blocked.add(m);
		}

		markerMaterial.setDiffuseColor(Color.RED);


		task = new AnimationTimer() {
			@Override
			public void handle(long now) {

				if((tms - map.getLastUpdate()) > 200 && map.getLastUpdate() != -1)
					return;

				if(map.isEmpty()) {
					if(!boxes.isEmpty()) {
						root.getChildren().removeAll(boxes.values());
						boxes.clear();
					}
					return;
				}
				map.getLatestMapItems(tms).forEachRemaining((p) -> {  addBlock(p); });
				setIndicator(map.getIndicator());
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

			if(boxes.containsKey(h))
				return;

			info.mapToGlobal(pos, global);

			// TODO: Fix rotation
			final Box box = new Box(size, size, size);
			box.setTranslateZ(global.x*100);
			box.setTranslateX(-global.y*100);
			box.setTranslateY((-global.z+info.getCellSize()/2)*100);
			box.setMaterial(blocked.get((int)(pos.probability*5)-1));
			box.setCullFace(CullFace.BACK);
			root.getChildren().add(box);
			boxes.put(h, box);
			return;

		} 

		else {

			if(!boxes.containsKey(h))
				return;

			//	if(pos.probability < 0.1f || pos.probability == 0.5f) {
			Box box = boxes.remove(h);
			if(box!=null) {
				root.getChildren().remove(box);	
			} 
		}
	}
	
	public void addBlock(Long h) {

        double p = info.decodeMapPoint(h, local);
		
		if(p > 0.5) {

			if(boxes.containsKey(h))
				return;

			info.mapToGlobal(local, global);

			// TODO: Fix rotation
			final Box box = new Box(size, size, size);
			box.setTranslateZ(global.x*100);
			box.setTranslateX(-global.y*100);
			box.setTranslateY((-global.z+info.getCellSize()/2)*100);
			box.setMaterial(blocked.get((int)(p*5)-1));
			box.setCullFace(CullFace.BACK);
			root.getChildren().add(box);
			boxes.put(h, box);
			return;

		} 

		else {

			if(!boxes.containsKey(h))
				return;

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

	public Map3DSpacialInfo getMapInfo() {
		return info;
	}

	public void invalidate() {
		Platform.runLater(() -> {
			root.getChildren().removeAll(boxes.values());
			boxes.clear();
			map.getLatestMapItems(0).forEachRemaining((p) -> { addBlock(p); });
		});
	}

	public void setIndicator(Point3D_F64 p) {
		
		boolean visible = !(Double.isNaN(p.x) || Double.isNaN(p.y) || Double.isNaN(p.z));
		
		Box box; Point3D_I32 local = new Point3D_I32();
		info.globalToMap(p, local);
		long h = info.encodeMapPoint(local, 0);
		if(boxes.containsKey(h)) {
			if(currentMarker.compare(h) || !visible) {
				box = boxes.get(currentMarker.h);
				if(box!=null) box.setMaterial(currentMarker.material);
			}
			if(visible) {
				box = boxes.get(h);
				currentMarker.set(h, box.getMaterial());
				box.setMaterial(markerMaterial);
			}
		}
	}


	private class Marker {
		public long h;
		public Material material;

		public void set(long h, Material material) {
			this.h = h;
			this.material = material;
		}

		public boolean compare(long m) {
			return h > 0 && h != m;
		}
	}




}
