package com.comino.flight.model.map;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;

import georegression.struct.point.Point3D_I32;

public class MAV2DMap implements IMAVMap {

	private final DataModel                  model;
	private final Map<Integer,Point3D_I32>   map;
	private final HashSet<Integer>           set;

	private int cx = 0;
	private int cy = 0;
	private int cz = 0;

	private int dimension = 400;
	private int resolution_cm = 5;
	
	public MAV2DMap(IMAVController control) {

		this.model = control.getCurrentModel();
		this.map   = new ConcurrentHashMap<Integer, Point3D_I32>();
		this.set   = new HashSet<Integer>();

		this.dimension = (int)(20.0f/0.05f)*2;
		this.resolution_cm = (int)(0.05f*100f);

		this.cx = dimension / 2;
		this.cy = dimension / 2;
		this.cz = dimension / 2;


		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_msp_micro_grid) {
				if(model.grid.hasTransfers()) 
					mapItemWorker(model.grid.getTransfers());
			}
		});	
	}


	public Map<Integer, Point3D_I32> getMap() {
		return map;
	}

	@Override
	public void clear() {
		map.clear();
	}
	
	@Override
	public Set<Integer> keySet() {
		return map.keySet();
	}
	
	@Override
	public void forEach(BiConsumer<Integer,Point3D_I32 > consumer) {
		for(Entry<Integer, Point3D_I32> entry : map.entrySet()) {
			consumer.accept(entry.getKey(),entry.getValue());
		}
	}
	
	@Override
	public Set<Integer> keySet(Comparable<Integer> zfilter) {
		if(zfilter==null)
			return map.keySet();
		set.clear();
		for(Entry<Integer, Point3D_I32> entry : map.entrySet()) {
			if(zfilter.compareTo(entry.getValue().z)==0)
			 set.add(entry.getKey());
		}
		return set;	
	}
	
	@Override
	public void forEach(Comparable<Integer> zfilter, BiConsumer<Integer,Point3D_I32 > consumer) {
		for(Entry<Integer, Point3D_I32> entry : map.entrySet()) {
			if(zfilter==null || zfilter.compareTo(entry.getValue().z)==0)
			  consumer.accept(entry.getKey(),entry.getValue());
		}
	}

	private void mapItemWorker(LinkedList<Integer> transfers) {
		int mpi= 0;
		while(!transfers.isEmpty()) {
			mpi = transfers.poll();
			if(map.containsKey(mpi))
				continue;
			if(mpi > 0) 
				map.put(mpi, decodeMapPoint(mpi));
			else 
				map.remove(-mpi);	
		}
	}

	private Point3D_I32 decodeMapPoint(int mpi) {

		Point3D_I32 p = new Point3D_I32();
		p.x = ((int)(mpi % dimension)-cx)*resolution_cm;
		p.y = ((int)((mpi / dimension) % dimension)-cy)*resolution_cm;
		p.z = ((int)(mpi  / (dimension* dimension))-cz)*resolution_cm;

		return p;
	}


}
