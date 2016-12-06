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


package com.comino.flight.model.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.comino.flight.log.ulog.ULogFromMAVLinkReader;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.observables.StateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.main.control.listener.IMAVMessageListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

public class AnalysisModelService implements IMAVLinkListener {

	private static AnalysisModelService instance = null;

	public static  final int STOPPED		 	= 0;
	public static  final int PRE_COLLECTING 	= 1;
	public static  final int COLLECTING     	= 2;
	public static  final int POST_COLLECTING    = 3;

	private DataModel								  model   = null;
	private ULogFromMAVLinkReader                   ulogger   = null;
	private AnalysisDataModel				    	current   = null;
	private AnalysisDataModel                        record   = null;
	private List<AnalysisDataModel> 		      modelList   = null;
	private StateProperties                           state   = null;

	private AnalysisDataModelMetaData                  meta  =  null;
	private List<ICollectorRecordingListener>    listener  =  null;

	private int     mode = 0;

	private  int  totalTime_sec = 30;
	private  int collector_interval_us = 25000;

	public static AnalysisModelService getInstance(IMAVController control) {
		if(instance==null)
			instance = new AnalysisModelService(control);
		return instance;
	}

	public static AnalysisModelService getInstance() {
		return instance;
	}


	private AnalysisModelService(IMAVController control) {

		this.meta = AnalysisDataModelMetaData.getInstance();
		this.listener = new ArrayList<ICollectorRecordingListener>();

		this.modelList     = new ArrayList<AnalysisDataModel>(50000);
		this.model         = control.getCurrentModel();
		this.current       =  new AnalysisDataModel();
		this.record       =  new AnalysisDataModel();
		this.state         = StateProperties.getInstance();

		this.ulogger = new ULogFromMAVLinkReader(control);

		control.addMAVLinkListener(this);

//		ExecutorService.get().scheduleAtFixedRate(new Converter(), 0, collector_interval_us, TimeUnit.MICROSECONDS);

	//	Thread c = new Thread(new Converter());
		Thread c = new Thread(new CombinedConverter());
	//	c.setPriority(Thread.MAX_PRIORITY);
		c.start();
	}

	public AnalysisModelService(DataModel model) {
		this.modelList     = new LinkedList<AnalysisDataModel>();
		this.model         =  model;
		this.current       =  new AnalysisDataModel();
		this.state         = StateProperties.getInstance();
		Thread c = new Thread(new CombinedConverter());
		c.setPriority(Thread.MAX_PRIORITY);
		c.start();
	}

	public void registerListener(ICollectorRecordingListener l) {
		listener.add(l);
	}

	public void setCollectorInterval(int interval_us) {
		this.collector_interval_us = interval_us;
	}


	public List<AnalysisDataModel> getModelList() {
		return modelList;
	}

	public AnalysisDataModel getCurrent() {
		return current;
	}

	public AnalysisDataModel getLast(float f) {
		if(mode==STOPPED && modelList.size()>0)
			return modelList.get(calculateX1Index(f));
		return current;
	}

	public void setCurrent(int index) {
		if(modelList.size() > index) {
			current = modelList.get(index);
		}
	}

	public int getCollectorInterval_ms() {
		return collector_interval_us/1000;
	}

	public boolean start() {

		if(mode==PRE_COLLECTING) {
			mode = COLLECTING;
			return true;
		}
		if(mode==STOPPED) {
			modelList.clear();
			mode = COLLECTING;
		}
		return mode != STOPPED;
	}


	public boolean stop() {
		mode = STOPPED;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {

		}
		return false;
	}

	public void stop(int delay_sec) {
		mode = POST_COLLECTING;
		ExecutorService.get().schedule(new Runnable() {
			@Override
			public void run() {
				mode = STOPPED;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}, delay_sec, TimeUnit.SECONDS);
	}

	public void setModelList(List<AnalysisDataModel> list) {
		mode = STOPPED;
		modelList.clear();
		list.forEach((e) -> {
			e.calculateVirtualKeyFigures(meta);
			modelList.add(e);

		});
	}

	public void dumpUlogFields() {
		List<String> sortedKeys=new ArrayList<String>(ulogger.getFieldList().keySet());
		Collections.sort(sortedKeys);
		sortedKeys.forEach((e) -> {
			System.out.print(e);
			meta.getKeyFigures().forEach((k) -> {
				if(k.sources.get(KeyFigureMetaData.ULG_SOURCE)!=null) {
					if(k.sources.get(KeyFigureMetaData.ULG_SOURCE).field.equals(e)) {
						System.out.print("\t\t\t\t=> mapped to "+k.desc1);
					}
				}
			});
			System.out.println();
		});
	}

	public void clearModelList() {
		mode = STOPPED;
		modelList.clear();
	}

	public void setTotalTimeSec(int totalTime) {
		this.totalTime_sec = totalTime;
	}

	public int getTotalTimeSec() {
		return totalTime_sec;
	}

	public int calculateX0Index(double factor) {
		int current_x0_pt = (int)((modelList.size() - totalTime_sec *  1000f / getCollectorInterval_ms()) * factor);

		if(current_x0_pt<0)
			current_x0_pt = 0;

		return current_x0_pt;
	}

	public int calculateX1Index(double factor) {

		int current_x1_pt = calculateX0Index(factor) + (int)(totalTime_sec *  1000f / getCollectorInterval_ms());

		if(current_x1_pt>modelList.size()-1)
			current_x1_pt = modelList.size()-1;

		return (int)(current_x1_pt);
	}

	public long getTotalRecordingTimeMS() {
		if(modelList.size()> 0)
			return (modelList.get(modelList.size()-1).tms) / 1000;
		else
			return 0;
	}


	public boolean isCollecting() {
		return mode != STOPPED ;
	}


	public int getMode() {
		return mode;
	}

	@Override
	public void received(Object _msg) {
		record.setValues(KeyFigureMetaData.MAV_SOURCE,_msg,meta);
	}


	private class CombinedConverter implements Runnable {

		long tms = 0; long wait = 0; int old_mode=STOPPED;
		float perf = 0;  AnalysisDataModel m = null;

		@Override
		public void run() {
			try { Thread.sleep(5000); } catch(Exception e) { }
			while(true) {

				current.setValue("MAVGCLACC", (System.nanoTime()-wait)/1e6f);
				current.setValue("MAVGCLPERF", perf);

				current.msg = null; wait = System.nanoTime();
				current.setValues(KeyFigureMetaData.MSP_SOURCE,model,meta);


				if(ulogger.isLogging()) {
					//	record.setValues(KeyFigureMetaData.MSP_SOURCE,model,meta);
					record.setValues(KeyFigureMetaData.ULG_SOURCE,ulogger.getData(), meta);
					record.calculateVirtualKeyFigures(AnalysisDataModelMetaData.getInstance());
				}
				if(model.msg != null && model.msg.tms > tms) {
					current.msg = model.msg; record.msg = model.msg;
					tms = current.msg.tms+100;
				} else {
					current.msg = null; record.msg = null;
				}

				current.calculateVirtualKeyFigures(AnalysisDataModelMetaData.getInstance());

				if(mode!=STOPPED && old_mode == STOPPED) {
					state.getLogLoadedProperty().set(false);
					state.getRecordingProperty().set(true);
					ulogger.enableLogging(true);
				}

				if(mode!=STOPPED) {
					if(ulogger.isLogging())
						m = record.clone();
					else
						m = current.clone();
					m.tms = System.nanoTime() / 1000 - tms;
                    m.dt_sec = m.tms / 1e6f;
					modelList.add(m);

					for(ICollectorRecordingListener updater : listener)
						updater.update(System.nanoTime());
				}

				if(mode==STOPPED && old_mode != STOPPED) {
					ulogger.enableLogging(false);
					state.getRecordingProperty().set(false);
				}

				perf = (collector_interval_us*1000 - (System.nanoTime()-wait))/1e6f;

				old_mode = mode;
				LockSupport.parkNanos(collector_interval_us*1000 - (System.nanoTime()-wait) - 3500000);
			}
		}
	}

}
