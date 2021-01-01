package com.comino.flight.model.map;

import java.util.Set;
import java.util.function.BiConsumer;

import georegression.struct.point.Point3D_I32;

public interface IMAVMap {
	
	public void forEach(BiConsumer<Integer,Point3D_I32 > consumer);
	public Set<Integer> keySet();
	public void clear();

}
