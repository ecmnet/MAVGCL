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

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.LocalMap3D;
import com.comino.mavmap.map.map3D.Map3DSpacialInfo;
import com.comino.mavutils.legacy.ExecutorService;

import bubo.maps.d3.grid.CellProbability_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point3D_I32;

public class MAVGCLMap  {
	
	private static MAVGCLMap mav2dmap = null;

	private final DataModel  model;  // Todo: Get rid of the current model
	
	private final LocalMap3D map;
	private final Point3D_I32 p          = new Point3D_I32();
	
	private final HashSet<Long> set      = new HashSet<Long>();

	private long last_update = - 1;
	
	
	public static MAVGCLMap getInstance(IMAVController control) {
		if(mav2dmap==null)
			mav2dmap = new MAVGCLMap(control);
		return mav2dmap;
	}
	
	public static MAVGCLMap getInstance() {
		return mav2dmap;
	}
	
	private MAVGCLMap(IMAVController control) {
		
		this.map   = new LocalMap3D(new Map3DSpacialInfo(0.10f,20.0f,20.0f,5.0f),false);
		this.model = control.getCurrentModel();
		
		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_msp_micro_grid) {
				LinkedList<Long> list = model.grid.getTransfers();
				while(!list.isEmpty()) {
					double probabiliy = map.getMapInfo().decodeMapPoint(list.pop(), p);
					map.setMapPoint(p, probabiliy);
				}
				
			  // TODO: Access AnalysisDatamodel
			  map.setIndicator(model.grid.ix, model.grid.iy, model.grid.iz);
			  last_update  = System.currentTimeMillis();
			}
		});
		
	}
	
	public Iterator<CellProbability_F64> getMapLevelItems() {
	  float current_altitude = (float)AnalysisModelService.getInstance().getCurrent().getValue("ALTRE");
	  float delta = 2.0f*map.getMapInfo().getCellSize();
	  return map.getMapLevelItems(current_altitude-delta,current_altitude+delta);
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
	
	public Point3D_F64 getIndicator() {
		return map.getIndicator();
	}
	
	public void clear() {
	    last_update = - 1;
		map.clear();	
	}
	
	public LocalMap3D getMap() {
		return map;
	}
	
	public long getLastUpdate() {
		return last_update;
	}
	
	public boolean isEmpty() {
		return map.size() == 1;
	}
	

	

}
