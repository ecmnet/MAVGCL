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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.LogMessage;
import com.comino.mavcom.model.segment.Status;

public class AnalysisDataModel implements Cloneable {

	public long       tms  = 0;
	public LogMessage msg  = null;
	public Status   status = null;

	public float    dt_sec = 0;

	private volatile Map<Integer,Double> data = null;
	private static List<Long> grid = new ArrayList<Long>();

	public AnalysisDataModel() {
		this.data = new HashMap<Integer,Double>();
//		this.grid = new ArrayList<Long>();
	}

	private AnalysisDataModel(Map<Integer,Double> d, List<Long> grid) {
		this.data = new HashMap<Integer,Double>();
		this.data.putAll(d);
//		this.grid = new ArrayList<Long>();
//		this.grid.addAll(grid);
	}


	public Object clone() {
	   AnalysisDataModel d = new AnalysisDataModel(data, grid);

		d.tms = tms;
		if(msg!=null)
			d.msg = msg.clone();
		if(status!=null)
			d.status = status.clone();
		return d;
	}

	public void set(AnalysisDataModel model) {
		this.data.clear();
		this.data.putAll(model.data);
//		this.grid.clear();
//		this.grid.addAll(model.grid);
	}

	public void clear()  {
		data.clear();
		grid.clear();
		tms = 0;
		msg = null;
		status = null;
	}
	
	public List<Long> getGrid() {
		return grid;
	}

	public double getValue(String kf) {
		int hash = kf.toLowerCase().hashCode();
		if(data!=null && data.containsKey(hash) && data.get(hash)!=null)
			return data.get(hash);
		else
			return 0;
	}

	public double getValue(KeyFigureMetaData m) {
		if(data != null && m!=null && data.containsKey(m.hash) && data.get(m.hash)!=null)
			return data.get(m.hash);
		else
			return Float.NaN;
	}

	public void setValue(String kf,double value) {
		if(data!=null)
		  data.put(kf.toLowerCase().hashCode(), value);
	}

	public void reset(AnalysisDataModelMetaData md) {
		if(data==null)
			return;
		md.getKeyFigureMap().forEach((i,e) -> {
			data.put(e.hash,Double.valueOf(0));
		});
	}

	private Double val = null;

	

	public  void  setValues(int type, Object source, AnalysisDataModelMetaData md ) {

		synchronized(this) {
			md.getKeyFigureMap().forEach((i,e) -> {
				val = Double.NaN;
				try {
					if(!e.isVirtual) {

						if(!e.hasSource(type))
							return;


						if( type == KeyFigureMetaData.MSP_SOURCE)
							val = e.getValueFromMSPModel((DataModel)source);
//						if( type == KeyFigureMetaData.PX4_SOURCE)
//							val = e.getValueFromPX4Model((Map<String,Object>)source);
						if( type == KeyFigureMetaData.ULG_SOURCE)
							val = e.getValueFromULogModel((Map<String,Object>)source);
//						if( type == KeyFigureMetaData.MAV_SOURCE)
//							val = e.getValueFromMAVLinkMessage(source);

						if(val!=null)
							data.put(e.hash,val);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					//				data.put(e.hash, Double.NaN);
				}
			});
		}
	}

	public void calculateVirtualKeyFigures(AnalysisDataModelMetaData md) {
		md.getVirtualKeyFigureMap().forEach((i,e) -> {
			try {
				if(e.isVirtual) {
					data.put(e.hash,e.calculateVirtualValue(this));
				}
			} catch (Exception e1) {
				data.put(e.hash, Double.NaN);
			}
		});
	}

}
