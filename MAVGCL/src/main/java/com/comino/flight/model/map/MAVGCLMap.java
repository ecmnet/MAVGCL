package com.comino.flight.model.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.flight.model.service.AnalysisModelService;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.Map3DSpacialInfo;

import bubo.maps.d3.grid.CellProbability_F64;
import georegression.struct.point.Point3D_F64;

public class MAVGCLMap  {

	private static int MAXMAPPOINTS   = 10000;

	private static MAVGCLMap mav2dmap = null;

	private final DataModel  model;  // Todo: Get rid of the current model

	private final Map3DSpacialInfo info;
	private final Point3D_F64 indicator       = new Point3D_F64();

	private final HashSet<Long>      set      = new HashSet<Long>();
	private final Map<Long,Long>  mapset      = new ConcurrentHashMap<Long,Long>(MAXMAPPOINTS);

	private float last_altitude  = -Float.MAX_VALUE;
	private long last_update = - 1;

	private int num_of_items = 0;


	public static MAVGCLMap getInstance(IMAVController control) {
		if(mav2dmap==null)
			mav2dmap = new MAVGCLMap(control);
		return mav2dmap;
	}

	public static MAVGCLMap getInstance() {
		return mav2dmap;
	}

	private MAVGCLMap(IMAVController control) {

		this.info =  new Map3DSpacialInfo(0.10f,20.0f,20.0f,5.0f);
		this.model = control.getCurrentModel();

		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_msp_micro_grid) {
				num_of_items = model.grid.count;
				if(model.grid.count == 0) {
					clear();
					return;
				}
				LinkedList<Long> list = model.grid.getTransfers();
				while(!list.isEmpty()) {
					Long entry = list.pop();
					if(entry >= 0) {
						if(!mapset.containsKey(entry) && mapset.size()<MAXMAPPOINTS)
							mapset.put(entry, System.currentTimeMillis());
					} else {
						mapset.remove(-entry);
					}
				}

				// TODO: Access AnalysisDatamodel
				indicator.set(model.grid.ix, model.grid.iy, model.grid.iz);
				last_update  = System.currentTimeMillis();
			}
		});

	}

	public Iterator<CellProbability_F64> getMapLevelItems(float current_altitude) {
		float delta = 2.0f*info.getCellSize();
		return new MapSetIterator( new ZFilter(current_altitude-delta,current_altitude+delta));
	}


	public Iterator<CellProbability_F64> getLatestMapItems(long tms) {
		return new MapSetIterator(tms);
	}

	public Set<Long> getLevelSet(boolean enforce) {

		float current_altitude = (float)AnalysisModelService.getInstance().getCurrent().getValue("ALTRE");
		set.clear();

		if(set.size()> 0 && Math.abs(current_altitude - last_altitude) < info.getCellSize() && !enforce)
			return set;

		set.clear();
		Iterator<CellProbability_F64> i = getMapLevelItems(current_altitude);
		while(i.hasNext()) {
			CellProbability_F64 p = i.next();
			set.add(info.encodeMapPoint(p, p.probability));
		}

		last_altitude = current_altitude;
		return set;	
	}

	public Map3DSpacialInfo getInfo() {
		return info;
	}

	public Point3D_F64 getIndicator() {
		return indicator;
	}

	public void clear() {
		last_update    = - 1;
		last_altitude  = -Float.MAX_VALUE;
		mapset.clear();
		num_of_items = 0;
	}

	public Map3DSpacialInfo getSpacialInfo() {
		return info;
	}


	public long getLastUpdate() {
		return last_update;
	}


	public boolean isEmpty() {
		return mapset.isEmpty();
	}

	public int size() {
		return mapset.size();
	}

	public boolean isComplete() {
		if(num_of_items == 0)
			return false;
		return ( (num_of_items - mapset.size()) * 100 / num_of_items ) < 5;
	}

	private class MapSetIterator implements Iterator<CellProbability_F64> {

		long tms; long next_tms;
		Comparable<Integer> zfilter = null;

		Iterator<Long> m = mapset.keySet().iterator();
		CellProbability_F64 storage = new CellProbability_F64();

		public MapSetIterator(long tms) {
			this.tms = tms;
			searchNext();
		}

		public MapSetIterator(Comparable<Integer> zfilter) {
			this.zfilter = zfilter;
			searchNext();
		}

		@Override
		public boolean hasNext() {
			return next_tms != 0;
		}

		@Override
		public CellProbability_F64 next() {
			return searchNext();
		}

		protected CellProbability_F64 searchNext() {
			next_tms = 0; 
			if(mapset.isEmpty())
				return storage;
			
			while(m.hasNext()) {
				long h = m.next(); next_tms = mapset.get(h);
				storage.probability = info.decodeMapPoint(h, storage);
				if(zfilter==null) {
					if ( storage.probability != 0.5f && next_tms > tms) 
						return storage;
				} else {
					if (storage.probability > 0.5f && next_tms > tms && zfilter.compareTo(storage.z) == 0) 
						return storage;			
				}
			}
			return storage;
		}

		@Override
		public void remove() {
			throw new RuntimeException("Remove is not supported");
		}
	}

	private class ZFilter implements Comparable<Integer> {

		int from;
		int to;

		public ZFilter(float from,float to) {

			this.from = (int)(from * info.getBlocksPerM());
			this.to   = (int)(to   * info.getBlocksPerM());
		}

		@Override
		public int compareTo(Integer z) {
			if( from < z &&  to > z) return 0; else return 1;

		}
	}
}
