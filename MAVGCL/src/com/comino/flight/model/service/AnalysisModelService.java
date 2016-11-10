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
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

public class AnalysisModelService implements IMAVLinkListener {

	private static AnalysisModelService instance = null;

	public static  final int STOPPED		 	= 0;
	public static  final int PRE_COLLECTING 	= 1;
	public static  final int COLLECTING     	= 2;
	public static  final int POST_COLLECTING    = 3;

	private static final int MODELCOLLECTOR_INTERVAL_US = 50000;

	private DataModel								  model   = null;
	private ULogFromMAVLinkReader                   ulogger   = null;
	private AnalysisDataModel				    	current   = null;
	private AnalysisDataModel                        record   = null;
	private ArrayList<AnalysisDataModel> 		  modelList   = null;
	private StateProperties                           state   = null;

	private AnalysisDataModelMetaData                  meta  =  null;

	private int     mode = 0;

	private  int  totalTime_sec = 30;

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

		this.modelList     = new ArrayList<AnalysisDataModel>();
		this.model         = control.getCurrentModel();
		this.current       =  new AnalysisDataModel();
		this.record       =  new AnalysisDataModel();
		this.state         = StateProperties.getInstance();

	    this.ulogger = new ULogFromMAVLinkReader(control);

		control.addMAVLinkListener(this);

		new Thread(new Converter()).start();
	}

	public AnalysisModelService(DataModel model) {
		this.modelList     = new ArrayList<AnalysisDataModel>();
		this.model         =  model;
		this.current       =  new AnalysisDataModel();
		this.state         = StateProperties.getInstance();
		new Thread(new Converter()).start();
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
		return MODELCOLLECTOR_INTERVAL_US/1000;
	}

	public boolean start() {

		if(mode==PRE_COLLECTING) {
			mode = COLLECTING;
			return true;
		}
		if(mode==STOPPED) {
			modelList.clear();
			mode = COLLECTING;
			 ulogger.enableLogging(true);
			new Thread(new Collector(0)).start();
		}
		return mode != STOPPED;
		//service = ExecutorService.get().scheduleAtFixedRate(new Collector(), 0, MODELCOLLECTOR_INTERVAL_US, TimeUnit.MICROSECONDS);
	}


	public boolean stop() {
		mode = STOPPED;
		ulogger.enableLogging(false);
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
				ulogger.enableLogging(false);
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
		ulogger.enableLogging(false);
		modelList.clear();
		list.forEach((e) -> {
			e.calculateVirtualKeyFigures(meta);
			modelList.add(e);

		});
	}

	public void clearModelList() {
		mode = STOPPED;
		modelList.clear();
		ulogger.enableLogging(false);
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

	public void start(int pre_sec) {
		if(mode==STOPPED) {
			modelList.clear();
            ulogger.enableLogging(true);
			mode = PRE_COLLECTING;
			new Thread(new Collector(pre_sec)).start();
		}
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

	private class Converter implements Runnable {

		@Override
		public void run() {
			long tms = 0; long wait = 0;
			while(true) {
				current.msg = null; wait = System.nanoTime();
				current.setValues(KeyFigureMetaData.MSP_SOURCE,model,meta);
				if(ulogger.isLogging())
			        record.setValues(KeyFigureMetaData.ULG_SOURCE,ulogger.getData(), meta);
				if(model.msg != null && model.msg.tms > tms) {
					current.msg = model.msg; record.msg = model.msg;
					tms = current.msg.tms+100;
				} else {
					current.msg = null; record.msg = null;
				}
				record.calculateVirtualKeyFigures(AnalysisDataModelMetaData.getInstance());
				LockSupport.parkNanos(MODELCOLLECTOR_INTERVAL_US*1000 - (System.nanoTime()-wait));
			}
		}
	}


	private class Collector implements Runnable {

		int pre_delay_count=0; int count = 0; long wait = 0;  AnalysisDataModel m = null;

		public Collector(int pre_delay_sec) {
			if(pre_delay_sec>0) {
				mode = PRE_COLLECTING;
				this.pre_delay_count = pre_delay_sec * 1000000 / MODELCOLLECTOR_INTERVAL_US;
			}
		}

		@Override
		public void run() {
			long tms = System.nanoTime() / 1000;
			state.getLogLoadedProperty().set(false);
			state.getRecordingProperty().set(true);
			while(mode!=STOPPED) {
				synchronized(this) {
					wait = System.nanoTime();
					if(ulogger.isLogging())
					  m = record.clone();
					else
					  m = current.clone();
					m.tms = System.nanoTime() / 1000 - tms;
					modelList.add(m);
					count++;
				}
				LockSupport.parkNanos(MODELCOLLECTOR_INTERVAL_US*1000 - (System.nanoTime()-wait));

				if(mode==PRE_COLLECTING) {
					int _delcount = count - pre_delay_count;
					if(_delcount > 0) {
						for(int i = 0; i < _delcount; i++ )
							modelList.remove(0);
					}

				}
			}
			state.getRecordingProperty().set(false);
		}

	}

}
