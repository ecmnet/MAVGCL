package com.comino.flight.model;

import java.util.HashMap;
import java.util.Map;

import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.LogMessage;
import com.comino.msp.model.segment.Status;

public class AnalysisDataModel {

	public long       tms  = 0;
	public LogMessage msg  = null;
	public Status   status = null;

	private Map<Integer,Float> data = null;

	public AnalysisDataModel() {
		this.data = new HashMap<Integer,Float>();
	}

	private AnalysisDataModel(Map<Integer,Float> d) {
		this.data = new HashMap<Integer,Float>();
		data.putAll(d);
	}

	public AnalysisDataModel clone() {
		AnalysisDataModel d = new AnalysisDataModel(data);
		d.tms = tms;
		d.msg = msg;
		d.status = status;
		return d;
	}

	public void clear()  {
		data.clear();
		tms = 0;
		msg = null;
		status = null;
	}


	public float getValue(String kf) {
		return data.get(kf.toLowerCase().hashCode());
	}

	public float getValue(KeyFigureMetaData m) {
		try {
			return data.get(m.hash);
		} catch(Exception e) { 	}
		return 0;
	}

	public void setValue(String kf,float value) {
		data.put(kf.toLowerCase().hashCode(), value);
	}

	public void setValues(DataModel m, AnalysisDataModelMetaData md) {
		md.getKeyFigureMap().forEach((i,e) -> {
			try {
				data.put(e.hash,e.getValueFromMSPModel(m));
			} catch (Exception e1) { }
		});
	}

	public void setValues(Map<String,Object> d, AnalysisDataModelMetaData md) {
		md.getKeyFigureMap().forEach((i,e) -> {
			try {
				data.put(e.hash,e.getValueFromPX4Model(d));
			} catch (Exception e1) { }
		});
	}


}
