package com.comino.flight.model.map;

import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.Map3DSpacialInfo;

import georegression.struct.point.Point3D_F64;

public class MAVGCLMap  {

	private static final long CLEARING      = -1L;      // A key -1 clears map
	private static final int MAXMAPPOINTS   = 30000;

	private static MAVGCLMap mav2dmap = null;

	private final DataModel  model;  

	private Map3DSpacialInfo info;
	private final Point3D_F64 indicator       = new Point3D_F64();

	private final HashSet<Long>      set      = new HashSet<Long>();
	private final BlockingQueue<Long>  list   = new ArrayBlockingQueue<Long>(MAXMAPPOINTS);

	private long  last_update = - 1;


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
				
				if(model.grid.count == 0) {
					clear();
					return;
				}

				// Adjust resolution
				if(info.getCellSize() != model.grid.resolution) {
					clear();
					info.adjustResolution(model.grid.resolution);
					System.out.println("Map resolution adjusted to "+model.grid.resolution+"m");
				}

				while(model.grid.hasTransfers()) {
					list.add(model.grid.pop());
				}

				// TODO: Access AnalysisDatamodel
				indicator.setTo(model.grid.ix, model.grid.iy, model.grid.iz);
				last_update  = System.currentTimeMillis();
			}
		});

	}
	
	public BlockingQueue<Long> getList() {
		return list;
	}


	public Map3DSpacialInfo getInfo() {
		return info;
	}

	public Point3D_F64 getIndicator() {
		return indicator;
	}

	public void clear() {
		last_update    = - 1;
		list.add(CLEARING);
	}

	public Map3DSpacialInfo getSpacialInfo() {
		return info;
	}


	public long getLastUpdate() {
		return last_update;
	}


	public boolean isEmpty() {
		return list.isEmpty();
	}

	public int size() {
		return list.size();
	}

	

//	private class MapSetIterator implements Iterator<CellProbability_F64> {
//
//		long tms; long next_tms = 0;
//		Comparable<Integer> zfilter = null;
//
//		Iterator<Long> m = mapset.keySet().iterator();
//
//		CellProbability_F64 storage = new CellProbability_F64();
//
//		public MapSetIterator(long tms) {
//			this.tms = tms;
//			searchNext();
//		}
//
//		public MapSetIterator(Comparable<Integer> zfilter) {
//			this.zfilter = zfilter;
//			searchNext();
//		}
//
//		@Override
//		public boolean hasNext() {
//			return next_tms != 0;
//		}
//
//		@Override
//		public CellProbability_F64 next() {
//			CellProbability_F64 prev = new CellProbability_F64();
//			prev.probability = storage.probability;
//			prev.setTo(storage);
//			searchNext();
//			return prev;
//		}
//
//		protected CellProbability_F64 searchNext() {
//
//			if(mapset.isEmpty()) {
//				next_tms = 0;
//				return storage;
//			}
//
//			while(m.hasNext()) {
//				long h = m.next(); 
//				if(!mapset.containsKey(h))
//					continue;
//				next_tms = mapset.get(h);
//				info.decodeMapPoint(h, storage);
//				if(zfilter==null) {
//					if ( next_tms >= tms)  {
//						if(storage.probability<=0.5) {
//							mapset.remove(h);
//						}
//						return storage;
//					}
//				} else {
//					if (zfilter.compareTo(storage.z) == 0) {
//						return storage;			
//					}
//				}
//			}
//			next_tms = 0;
//
//
//			return storage;
//		}
//
//		@Override
//		public void remove() {
//			throw new RuntimeException("Remove is not supported");
//		}
//	}
//
//	private class ZFilter implements Comparable<Integer> {
//
//		int from;
//		int to;
//
//		public ZFilter(float from,float to) {
//
//			this.from = (int)(from * info.getBlocksPerM());
//			this.to   = (int)(to   * info.getBlocksPerM());
//		}
//
//		@Override
//		public int compareTo(Integer z) {
//			if( from < z &&  to > z) return 0; else return 1;
//
//		}
//	}
}
