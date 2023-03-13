package com.comino.flight.ui.widgets.view3D.objects;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.comino.flight.model.map.MAVGCLOctoMap;
import com.comino.flight.ui.widgets.view3D.octomesh.OctoMesh;
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

	

	private final static long    KEYMASK      = 0x0FFFFFFFFFFFFFFFL;

	private final Group root;

	private AnimationTimer              task    = null;

	private final MAVGCLOctoMap         map;
	private Point4D_F32                 tmp;
	private final int                   size;
	private DataModel model;

	private Map<Long,OctoMesh>          meshIndex = new HashMap<>();
	private List<OctoMesh>              meshes    = new LinkedList<>();
	

	public Map3DOctoGroup(Group root, IMAVController control) {

		this.root  = root;
		this.model = control.getCurrentModel();
		this.map   = MAVGCLOctoMap.getInstance(control);
		this.tmp   = new Point4D_F32();
		this.size = (int)(map.getResolution() * 100f);
		

		task = new AnimationTimer() {
			long tms_old;
			@Override
			public void handle(long now) {

				// update rate 10Hz
				if((now - tms_old)<20_000_000L)
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

					// TODO: Find a thread safe way to access changes in the octomap
				} catch(ConcurrentModificationException c) { 
                    
				}
			}
		};
	}

	public void handleBlock(Point4D_F32 p, long id) {
		
		if(meshes.isEmpty())
			meshes.add(new OctoMesh(root,size));
		
		OctoMesh mesh =	meshes.get(meshes.size()-1);
		if(tmp.w > 0.5) {
			if(meshIndex.containsKey(id) || tmp.z < 0)
				return;
			if(!mesh.hasSpace()) {
				mesh = new OctoMesh(root,size);
				meshes.add(mesh);
			}
			mesh.add(id, -tmp.y*100, tmp.z*100, tmp.x*100);
			meshIndex.put(id, mesh);
		} else {
			mesh = meshIndex.remove(id);
			if(mesh!=null) {
				if(mesh.remove(id)) {
					meshes.remove(mesh);
				}
			}
		}
	}

	public void enable(boolean enable) {
		if(enable)
			task.start();
		else
			task.stop();
	}


	public void clear() {

		meshIndex.forEach((id,mesh) -> { 
			if(mesh.remove(id)) { 
				meshes.remove(mesh); 
			}
		});
		meshIndex.clear();
	}

}
