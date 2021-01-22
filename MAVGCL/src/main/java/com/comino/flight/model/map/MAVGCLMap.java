package com.comino.flight.model.map;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.LocalMap3D;
import com.comino.mavmap.map.map3D.Map3DSpacialInfo;
import com.comino.mavutils.legacy.ExecutorService;

import bubo.maps.d3.grid.CellProbability_F64;
import georegression.struct.point.Point3D_I32;

public class MAVGCLMap  {
	
	private static MAVGCLMap mav2dmap = null;

	private final DataModel  model;
	private final LocalMap3D map         = new LocalMap3D();
	private final Point3D_I32 p          = new Point3D_I32();
	
	private final HashSet<Long> set      = new HashSet<Long>();
	
	
	public static MAVGCLMap getInstance(IMAVController control) {
		if(mav2dmap==null)
			mav2dmap = new MAVGCLMap(control);
		return mav2dmap;
	}
	
	public static MAVGCLMap getInstance() {
		return mav2dmap;
	}
	
	private MAVGCLMap(IMAVController control) {

		this.model = control.getCurrentModel();
		
		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_msp_micro_grid) {
				LinkedList<Long> list = model.grid.getTransfers();
				while(!list.isEmpty()) {
					double probabiliy = map.getMapInfo().decodeMapPoint(list.pop(), p);
					map.setMapPoint(p, probabiliy);
				}
			}
		});
		
	}
	
	public Iterator<CellProbability_F64> getMapLevelItems() {
	  return map.getMapLevelItems( model.hud.ar-0.5f,model.hud.ar+0.5f);
	}
	
	public Iterator<CellProbability_F64> getLatestMapItems(long tms) {
		  return map.getLatestMapItems(tms);
		}
	
	public Set<Long> getLevelSet() {
		set.clear();
		Iterator<CellProbability_F64> i = getMapLevelItems();
		while(i.hasNext()) {
			CellProbability_F64 p = i.next();
			set.add(map.getMapInfo().encodeMapPoint(p, p.probability));
		}
		return set;	
	}

	public Map3DSpacialInfo getInfo() {
		return map.getMapInfo();
	}
	
	public void clear() {
		map.clear();	
	}
	
	public boolean isEmpty() {
		return map.size() == 1;
	}
	

	

}
