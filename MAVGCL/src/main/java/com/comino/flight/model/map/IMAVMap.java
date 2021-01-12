package com.comino.flight.model.map;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import georegression.struct.point.Point3D_I32;

public interface IMAVMap {
	
	public void forEach(BiConsumer<Integer,Point3D_I32 > consumer);
	public void forEach(Comparable<Integer> zfilter, BiConsumer<Integer,Point3D_I32 > consumer);
	public Set<Integer> keySet();
	public Set<Integer> keySet(Comparable<Integer> zfilter);
	public Map<Integer,Point3D_I32> getMap();
	public void clear();

}
