package com.comino.flight.model;

import java.util.HashMap;
import java.util.Map;

import com.comino.msp.model.DataModel;

public class AnalysisDataModel {

	public long tms = 0;

	private Map<Integer,Float> data = null;

	public AnalysisDataModel() {
		this.data = new HashMap<Integer,Float>();
	}

	private AnalysisDataModel(Map<Integer,Float> d) {
		this.data = new HashMap<Integer,Float>();
		data.putAll(d);
	}

	public AnalysisDataModel clone() {
		return new AnalysisDataModel(data);
	}

	public float getValue(String kf) {
		return data.get(kf.toLowerCase().hashCode());
	}

	public void setValue(String kf,float value) {
		data.put(kf.toLowerCase().hashCode(), value);
	}

	public void setValues(DataModel m, AnalysisDataModelMetaData md) {
         md.getKeyFigures().forEach((i,e) -> {
        	try {
				data.put(e.hash,e.getValueFromMSPModel(m));
			} catch (Exception e1) { }
         });
	}

	public void setValues(Map<String,Object> d, AnalysisDataModelMetaData md) {
        md.getKeyFigures().forEach((i,e) -> {
       	try {
				data.put(e.hash,e.getValueFromPX4Model(d));
			} catch (Exception e1) { }
        });
	}


}
