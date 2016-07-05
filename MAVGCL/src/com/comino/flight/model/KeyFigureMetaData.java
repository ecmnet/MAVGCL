package com.comino.flight.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import com.comino.msp.model.DataModel;

public class KeyFigureMetaData {

	public String desc1;
	public String desc2;
	public String uom;
	public String mask;
	public int    hash;

	private String mspclass;
	private String mspfield;
	private String px4field;


	public KeyFigureMetaData(String key, String desc, String uom, String mask) {
		this.desc1  = desc;
		this.uom    = uom;
		this.mask   = mask;
		this.hash   = key.toLowerCase().hashCode();
	}

	public void setMSPSource(String mspclass, String mspfield) {
		this.mspclass = mspclass;
		this.mspfield = mspfield;
	}

	public void setPX4Source(String px4field) {
		this.px4field = px4field;
		this.desc2    = px4field;
	}

	public float getValueFromMSPModel(DataModel m) throws Exception {
		Field mclass_field = m.getClass().getField(mspclass);
		Object mclass = mclass_field.get(m);
		Field mfield_field = mclass.getClass().getField(mspfield);
		return mfield_field.getFloat(mclass);
	}


	public float getValueFromPX4Model(Map<String,Object> data) {
		return (float) data.get(px4field);
	}


	public String toString() {
		return desc1+" ("+mspfield+","+px4field+")";
	}



}
