/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.flight.model.converter.SourceConverter;
import com.comino.msp.model.DataModel;

public class KeyFigureMetaData {

	public static final int MSP_SOURCE = 1;
	public static final int PX4_SOURCE = 2;
	public static final int ULG_SOURCE = 3;

	public String desc1;
	public String desc2;
	public String uom;
	public String mask;
	public int    hash;

	private String key;

	private Map<Integer,DataSource> sources = new HashMap<Integer,DataSource>();

	public KeyFigureMetaData() {
		this.desc1  = "None";
		this.hash   = 0;
	}


	public KeyFigureMetaData(String key, String desc, String uom, String mask) {
		this.desc1  = desc;
		this.uom    = uom;
		this.mask   = mask;
		this.key    = key;
		this.hash   = key.toLowerCase().hashCode();
	}

	public void setSource(int type,String field, String class_c, String[] params) {
		setSource(type,null,field,class_c,params);
	}

	public void setSource(int type, String class_n, String field, String class_c, String[] params) {
		if(class_c!=null) {
			try {
				SourceConverter conv = null;
				Class<?> clazz = Class.forName(this.getClass().getPackage().getName()+".converter."+class_c);
				conv = (SourceConverter) clazz.newInstance();
				conv.setParameter(key,params);
				sources.put(type, new DataSource(class_n,field,conv));
			} catch(Exception e) {
				System.err.println(this.getClass().getPackage().getName()+".converter."+type+" : "+e.getMessage());
			}
		} else
			sources.put(type, new DataSource(class_n,field,null));
	}

	public float getValueFromMSPModel(DataModel m) throws Exception {
		float value = 0;
		DataSource source = sources.get(MSP_SOURCE);
		Field mclass_field = m.getClass().getField(source.class_n);
		Object mclass = mclass_field.get(m);
		Field mfield_field = mclass.getClass().getField(source.field);
		value = new Double(mfield_field.getDouble(mclass)).floatValue();
		if(source.converter != null)
			return source.converter.convert(value);
		return value;
	}


	public float getValueFromPX4Model(Map<String,Object> data) {
		float value = 0;
		DataSource source = sources.get(PX4_SOURCE);
		Object o = data.get(source.field);
		if(o instanceof Integer)
			value = (float)(Integer)o;
		else if(o instanceof Double)
			value = ((Double)o).floatValue();
		else
			value = (float)(Float)o;

		if(source.converter != null)
			return source.converter.convert(value);
		return value;
	}

	public float getValueFromULogModel(Map<String,Object> data) {
		float value = 0;
		DataSource source = sources.get(ULG_SOURCE);
		Object o = data.get(source.field);
		if(o instanceof Integer)
			value = (float)(Integer)o;
		else if(o instanceof Double)
			value = ((Double)o).floatValue();
		else
			value = (float)(Float)o;

		if(source.converter != null)
			return source.converter.convert(value);
		return value;
	}

	public String toString() {
		return desc1;
	}

	public String toStringAll() {
		return desc1+": "+key+"("+hash+")";
	}

	private class DataSource {

		public DataSource(String class_n, String field, SourceConverter converter) {
			this.class_n = class_n;
			this.field   = field;
			this.converter = converter;
		}

		public String class_n;
		public String field;
		public SourceConverter converter;
	}



}
