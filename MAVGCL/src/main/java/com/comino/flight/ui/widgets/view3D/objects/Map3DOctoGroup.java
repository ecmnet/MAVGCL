package com.comino.flight.ui.widgets.view3D.objects;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.comino.flight.model.map.MAVGCLOctoMap;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;

import georegression.struct.point.Point4D_F32;
import javafx.animation.AnimationTimer;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import us.ihmc.jOctoMap.key.OcTreeKeyReadOnly;

public class Map3DOctoGroup  {

	private final static double MIN = 0 ;
	private final static double MAX = 10.0 ;
	private final static double BLUE_HUE = Color.BLUE.getHue() ;
	private final static double RED_HUE = Color.RED.getHue() ;

	private final static long    KEYMASK      = 0x0FFFFFFFFFFFFFFFL;


	private final Group root;

	private AnimationTimer              task    = null;
	private final List<PhongMaterial>  	blocked = new ArrayList<PhongMaterial>();
	private final Map<Key,Box>          boxes;
	private final List<Box>             box_add    = new ArrayList<Box>();
	private final List<Box>             box_remove = new ArrayList<Box>();

	private final MAVGCLOctoMap         map;
	private final Point4D_F32           tmp;
	private final int                   size;
	private DataModel model;



	public Map3DOctoGroup(Group root, IMAVController control) {


		this.root  = root;
		this.model = control.getCurrentModel();
		this.map   = MAVGCLOctoMap.getInstance(control);
		this.tmp   = new Point4D_F32();
		this.boxes = new HashMap<Key,Box>();
		this.size = (int)(map.getResolution() * 100f);

		for(int i = 0; i < (MAX/map.getResolution()); i++) {
			PhongMaterial m = new PhongMaterial();
			m.setDiffuseColor(getColorForValue(i*map.getResolution()));
			m.setSpecularColor(Color.WHITE);
			blocked.add(m);
		}

		task = new AnimationTimer() {
			long tms_old;
			@Override
			public void handle(long now) {

				// update rate 10Hz
				if((now - tms_old)<100_000_000L)
					return;
				tms_old = now;

				if(model.grid.count == -1) {
					model.grid.count = 0;
					clear();	
				}

				try {
					Iterator<OcTreeKeyReadOnly> list = map.getChanged().iterator();
					while(list!=null && list.hasNext()) {
						long id = map.convertTo(list.next(), tmp) & KEYMASK;
						handleBlock(tmp,id);
					}
					map.resetChangeDetection();
					
					root.getChildren().addAll(box_add);        box_add.clear();
					root.getChildren().removeAll(box_remove);  box_remove.clear();
					
					// TODO: Find a thread safe way to access changes in the octomap
				} catch(ConcurrentModificationException c) { 

				}
			}
		};
	}

	public void handleBlock(Point4D_F32 p, long id) {
		Key key = new Key(id);
		if(p.w > 0.5) {
			if(boxes.containsKey(key))
				return;

			final Box box = new Box(size, size, size);
			final int index = (int)(p.z/map.getResolution());

			box.setTranslateZ(p.x*100);
			box.setTranslateX(-p.y*100);
			box.setTranslateY(p.z*100);

			box.setMaterial(blocked.get(index > blocked.size()-1 ? 0 : index));
			box.setCullFace(CullFace.NONE);
			box.setDepthTest(DepthTest.ENABLE);
			boxes.put(key, box);
			box_add.add(box);
		} else {
			Box box = boxes.remove(key);
			if(box!=null) 
				box_remove.add(box);
		}
	}

//	private void removeBlock(long encoded) {
//		Box box = boxes.remove(encoded);
//		if(box!=null) 
//			root.getChildren().remove(box);	 
//	}

	public void enable(boolean enable) {
		if(enable)
			task.start();
		else
			task.stop();
	}


	public void clear() {
		boxes.forEach((id,b)-> {
			box_remove.add(b);
		});
		root.getChildren().removeAll(box_remove);  
		box_remove.clear();
		boxes.clear();

	}

	private Color getColorForValue(double value) {
		if (value < MIN || value > MAX) {
			return Color.BLACK ;
		}
		double hue = BLUE_HUE + (RED_HUE - BLUE_HUE) * (value - MIN) / (MAX - MIN) ;
		return Color.hsb(hue, 1.0, 1.0);
	}

	private class Key  {

		int id;

		public Key(long id) {
			this.id = (int)(id ^ (id >>> 32));
		}

		@Override
		public int hashCode() {
			return id;
		}

		public boolean equals(Object obj) {
			if (obj instanceof Key) {
				return id  == ((Key)obj).id;
			}
			return false;
		}


	}

}
