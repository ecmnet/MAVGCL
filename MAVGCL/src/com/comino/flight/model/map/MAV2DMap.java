package com.comino.flight.model.map;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;

import georegression.struct.point.Point3D_F32;

public class MAV2DMap implements IMAVMap {

	private final DataModel                  model;
	private final Map<Integer,Point3D_F32>   map;

	private int cx = 0;
	private int cy = 0;
	private int cz = 0;

	private int dimension = 400;
	private float resolution_cm = 5f;



	public MAV2DMap(IMAVController control) {

		this.model = control.getCurrentModel();
		this.map   = new ConcurrentHashMap<Integer, Point3D_F32>();
		
		this.dimension = (int)(20.0f/0.05f)*2;
		this.resolution_cm = (int)(0.05f*100f);
		
		this.cx = dimension / 2;
		this.cy = dimension / 2;
		this.cz = dimension / 2;
		
		
		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_msp_micro_grid) {
//				System.out.println(model.grid.getTransfers().size());
				if(model.grid.hasTransfers())
					mapItemWorker(model.grid.getTransfers());	
			}
		});	
	}

	@Override
	public Map<Integer, Point3D_F32> getMap() {
		return map;
	}

	@Override
	public void clear() {
		map.clear();
	}

	private void mapItemWorker(LinkedList<Integer> transfers) {
		int mpi= 0;
		while(!transfers.isEmpty()) {
			mpi = transfers.poll();
			if(map.containsKey(mpi))
				continue;
			if(mpi > 0) {
				map.put(mpi, decodeMapPoint(mpi));
			} else 
				if(mpi <= 0)
					map.remove(-mpi);	
		}
	}

	private Point3D_F32 decodeMapPoint(int mpi) {
		
		Point3D_F32 p = new Point3D_F32();
		p.x = ((int)(mpi % dimension)-cx)*resolution_cm/100f;
		p.y = ((int)((mpi / dimension) % dimension)-cy)*resolution_cm/100f;
		p.z = ((int)(mpi  / (dimension* dimension))-cz)*resolution_cm/100f;
	
		return p;
	}

}
