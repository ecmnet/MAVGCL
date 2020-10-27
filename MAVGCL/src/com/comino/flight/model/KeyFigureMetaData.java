/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

import com.comino.flight.model.converter.SourceConverter;
import com.comino.mavcom.model.DataModel;

public class KeyFigureMetaData {

	private static final DecimalFormat f1 = new DecimalFormat("#0.0");
	private static final DecimalFormat f2 = new DecimalFormat("#0.00");
	private static final DecimalFormatSymbols sym = new DecimalFormatSymbols();

	{
		sym.setNaN("-");
		f1.setDecimalFormatSymbols(sym);
		f2.setDecimalFormatSymbols(sym);

	}

	public static final int MSP_SOURCE = 1;
	public static final int PX4_SOURCE = 2;
	public static final int ULG_SOURCE = 3;
	public static final int MAV_SOURCE = 4;

	public static final int VIR_SOURCE = 9;

	public String desc1;
	public String desc2;
	public String uom;
	//	public String mask;
	public int    hash;
	public float  min=0;
	public float  max=0;
	public double  clip_min = -Double.MAX_VALUE;
	public double  clip_max =  Double.MAX_VALUE;

	private DecimalFormat formatting = null;

	private double value = 0;
	private DataSource source = null;

	public boolean isVirtual = false;

	private String key;

	public Map<Integer,DataSource> sources = new HashMap<Integer,DataSource>();

	public KeyFigureMetaData() {
		this.desc1  = "None";
		this.hash   = 0;
	}


	public KeyFigureMetaData(String key, String desc, String uom, String mask) {
		this.desc1  = desc;
		this.uom    = uom;
		this.key    = key;
		this.hash   = key.toLowerCase().hashCode();

		if(mask!=null && !mask.equalsIgnoreCase("auto")) {
			formatting = new DecimalFormat(mask);
			formatting.setDecimalFormatSymbols(sym);
		}

	}

	public String getValueString(double val) {
		if(uom!=null && uom.equals("%"))
			return Integer.toString((int)(val*100f));
		if(formatting!=null)
			return formatting.format(val);
		if(Math.abs(val)<100)
			return f2.format(val);
		if(Math.abs(val)<1000)
			return f1.format(val);
		return String.valueOf(val);
	}

	public void setBounds(float min, float max) {
		if(Float.isFinite(min) && Float.isFinite(max)) {
			this.min = min;
			this.max = max;
		}
	}

	public void setClipping(float min, float max) {
		if(Float.isFinite(min) && Float.isFinite(max)) {
			this.clip_min = (double)min;
			this.clip_max = (double)max;
		}
	}

	public void setSource(int type,String field, String class_c, String[] params) {
		setSource(type,null,field,class_c,params);

	}

	public void setSource(int type, String class_n, String field, String class_c, String[] params) {

		if(type==VIR_SOURCE) isVirtual = true;

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

	public boolean hasSource(int type) {
		return sources.containsKey(type);
	}

	public Double getValueFromMSPModel(DataModel m) throws Exception {
		value = Double.NaN;
		source = sources.get(MSP_SOURCE);
		if(source.field!=null) {
			Field mclass_field = m.getClass().getField(source.class_n);
			Object mclass = mclass_field.get(m);
			try {
				Field mfield_field = mclass.getClass().getField(source.field);
				value = mfield_field.getDouble(mclass);
			} catch(NoSuchFieldException e) {
				return value;
			} catch(NullPointerException e) {
				return value;
			}
		}
		if(source.converter != null)
			return checkClipping(source.converter.convert(value));
		return checkClipping(value);
	}


//	public Double getValueFromPX4Model(Map<String,Object> data) {
//		value = Double.NaN;
//		source = sources.get(PX4_SOURCE);
//		if(source.field!=null) {
//			Object o = data.get(source.field);
//			if(o instanceof Integer)
//				value = (float)(Integer)o;
//			else if(o instanceof Double)
//				value = ((Double)o).doubleValue();
//			else
//				value = (float)(Float)o;
//		}
//		if(source.converter != null)
//			return checkClipping(source.converter.convert(value));
//		return checkClipping(value);
//	}

	public Double getValueFromULogModel(Map<String,Object> data) {
		value = Double.NaN;
		source = sources.get(ULG_SOURCE);

		if(source!=null && source.field!=null) {  // source field specified
			Object o = data.get(source.field);
			if(o!=null) {
				if(o instanceof Integer)
					value = (double)(Integer)o;
				else if(o instanceof Double)
					value = ((Double)o).doubleValue();
				else if(o instanceof Float)
					value = ((Float)o).doubleValue();

				if(source.converter != null)
					return checkClipping(source.converter.convert(value));

			}

		} else { // source field via converter
			if(source.converter != null)
				return checkClipping(source.converter.convert(data));
		}
		return checkClipping(value);
	}

//	public Double getValueFromMAVLinkMessage(Object mavlink_message) throws Exception {
//		value = Double.NaN;
//		source = sources.get(MAV_SOURCE);
//		if(source.field!=null) {
//			if(mavlink_message.getClass().getSimpleName().equals(source.class_n)) {
//				Field mfield_field = mavlink_message.getClass().getField(source.field);
//				value = mfield_field.getDouble(mavlink_message);
//			}
//			if(source.converter != null)
//				return checkClipping(source.converter.convert(value));
//			return checkClipping(value);
//		}
//		return null;
//	}

	public double calculateVirtualValue(AnalysisDataModel data) {
		source = sources.get(VIR_SOURCE);
		if(source.converter != null)
			return checkClipping(source.converter.convert(data));
		return 0;
	}

	public String toString() {
		return desc1;
	}

	public String toStringAll() {
		return desc1+": "+key+"("+hash+")";
	}

	private Double checkClipping(double v) {
		if(v > clip_max) v = Double.NaN;
		if(v < clip_min) v = Double.NaN;
		return v;
	}

	public class DataSource {

		public DataSource(String class_n, String field, SourceConverter converter) {
			this.class_n = class_n;
			this.field   = field;
			this.converter = converter;
		}

		public String class_n;
		public String field;
		public SourceConverter converter;

		public String toString() {
			if(class_n!=null)
				return class_n+"."+field;
			return field;
		}
	}
}
