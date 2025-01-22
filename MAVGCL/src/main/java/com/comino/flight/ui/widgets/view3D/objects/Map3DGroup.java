package com.comino.flight.ui.widgets.view3D.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.comino.flight.model.map.MAVGCLMap;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.Map3DSpacialInfo;

import bubo.maps.d3.grid.CellProbability_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point3D_I32;
import javafx.animation.AnimationTimer;
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
	private final Map<Long,Box>         boxes;

	private final PhongMaterial markerMaterial  = new PhongMaterial();

	private final MAVGCLMap             map;
	private final Map3DSpacialInfo      info;
	private double                      size = 0;

	private final Point3D_F64           global = new Point3D_F64();

	private Marker currentMarker = new Marker();

	private CellProbability_F64 point = new CellProbability_F64();
	
	private int added = 0;
	private int removed=0;


	public Map3DGroup(Group root, DataModel model) {

		this.root  = root;
		this.map   = MAVGCLMap.getInstance();
		this.boxes = map.getMap();
		this.info  = map.getInfo();
		this.size  = info.getCellSize() * 100;

		for(int i = 0; i < 5; i++) {
			PhongMaterial m = new PhongMaterial();
			m.setDiffuseColor(Color.web("DARKCYAN", 1));
			m.setSpecularColor(Color.WHITE);
			blocked.add(m);
		}

		markerMaterial.setDiffuseColor(Color.RED);


		task = new AnimationTimer() {
			long entry=0; long tms_old;
			@Override
			public void handle(long now) {
				
				if((now - tms_old)<20000000)
					return;
				tms_old = now;
				
				BlockingQueue<Long> list = map.getList();

				if(!list.isEmpty()) {
					
					size = info.getCellSize() * 100;
					
					while(!list.isEmpty()) {

						entry = list.poll();
						if(entry < 0) {
							root.getChildren().removeAll(boxes.values());
							boxes.clear();
							list.clear();
							System.out.println("Clearing");
							return;
						}
						info.decodeMapPoint(entry, point);
						addBlock(point);
					}
					setIndicator(map.getIndicator());
				}
			//	System.out.println("Added: "+added+" Removed: "+removed+" Total: "+(added+removed));
			}	
		};

		// Always rung grid update
		task.start();

	}

	public void addBlock(CellProbability_F64 pos) {

		long h = info.encodeMapPoint(pos,0);
		
		added++;
		
		if(pos.probability > 0.5) {

			if(boxes.containsKey(h))
				return;
			
			

			info.mapToGlobal(pos,map.getOrigin(), global);

			// TAsk:Fix rotation
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
			
			removed++;

			//	if(pos.probability < 0.1f || pos.probability == 0.5f) {
			Box box = boxes.remove(h);
			if(box!=null) {
				
				root.getChildren().remove(box);	
			} 
		}

	}



	public void clear() {
		added = 0; removed=0;
		root.getChildren().clear();
		boxes.clear();

	}

	public Map3DSpacialInfo getMapInfo() {
		return info;
	}

	public void invalidate() {
		//		Platform.runLater(() -> {
		//			root.getChildren().removeAll(boxes.values());
		//			boxes.clear();
		//			map.getLatestMapItems(0).forEachRemaining((p) -> { addBlock(p); });
		//		});
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
