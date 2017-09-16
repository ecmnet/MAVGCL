/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.MSP_CMD;

import com.comino.flight.log.ulog.ULogFromMAVLinkReader;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.observables.StateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

public class AnalysisModelService implements IMAVLinkListener {

	private static AnalysisModelService instance = null;

	public static  final int STOPPED		 	= 0;
	public static  final int PRE_COLLECTING 	= 1;
	public static  final int COLLECTING     	= 2;
	public static  final int POST_COLLECTING    = 3;
	public static  final int READING_HEADER     = 4;

	private volatile List<AnalysisDataModel>      modelList   = null;

	private DataModel								  model   = null;
	private ULogFromMAVLinkReader                   ulogger   = null;
	private AnalysisDataModel				    	current   = null;
	private AnalysisDataModel                        record   = null;
	private StateProperties                           state   = null;

	private AnalysisDataModelMetaData                  meta  =  null;
	private List<ICollectorRecordingListener>    listener  =  null;

	private VehicleHealthCheck health = null;

	private int mode = 0;

	private boolean isFirst = false;

	private int totalTime_sec = 30;
	private int collector_interval_us = 50000;
	private IMAVController control = null;

	public static AnalysisModelService getInstance(IMAVController control) {
		if(instance==null)
			instance = new AnalysisModelService(control);
		return instance;
	}

	public static AnalysisModelService getInstance() {
		return instance;
	}


	private AnalysisModelService(IMAVController control) {

		this.control = control;
		this.health = new VehicleHealthCheck(control);

		this.meta = AnalysisDataModelMetaData.getInstance();
		this.listener = new ArrayList<ICollectorRecordingListener>();

		this.modelList     = new ArrayList<AnalysisDataModel>(50000);
		this.model         = control.getCurrentModel();
		this.current       =  new AnalysisDataModel();
		this.record        =  new AnalysisDataModel();
		this.state         = StateProperties.getInstance();

		this.ulogger = new ULogFromMAVLinkReader(control);

		control.addMAVLinkListener(this);

		state.getConnectedProperty().addListener((o,ov,nv) -> {
			if(nv.booleanValue()) {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
				if(!control.isSimulation()) {
					model.grid.clear();
					control.sendMSPLinkCmd(MSP_CMD.MSP_TRANSFER_MICROSLAM);
					MSPLogger.getInstance().writeLocalMsg("[mgc] grid data requested",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				}
			}
		});

		Thread c = new Thread(new CombinedConverter());
		c.setName("Combined model converter");
		c.start();
	}

	public AnalysisModelService(DataModel model) {
		this.modelList     = new LinkedList<AnalysisDataModel>();
		this.model         =  model;
		this.current       =  new AnalysisDataModel();
		this.state         = StateProperties.getInstance();

		Thread c = new Thread(new CombinedConverter());
		c.setName("Combined model converter");
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
			return modelList.get(calculateX1IndexByFactor(f));
		return current;
	}

	public void setCurrent(int index) {
		if(modelList.size() > index-1) {
			if(index < 0)
				return;
			current.set(modelList.get(index));
		}
	}

	public void setCurrent(double time) {
		setCurrent(calculateXIndexByTime(time));
	}

	public int getCollectorInterval_ms() {
		return collector_interval_us/1000;
	}

	public boolean start() {

		if(!control.isConnected()) {
			return false;
		}

		this.isFirst=true;

		//		if(mode==PRE_COLLECTING) {
		//			mode = COLLECTING;
		//			return true;
		//		}

		if(mode==STOPPED) {
			modelList.clear();
			mode = COLLECTING;
			return true;
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
					Thread.sleep(500);
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

	public void clearModelList() {
		mode = STOPPED;
		current.clear();
		modelList.clear();
	}

	public void setTotalTimeSec(int totalTime) {
		this.totalTime_sec = totalTime;
	}

	public int getTotalTimeSec() {
		return totalTime_sec;
	}

	public int calculateX0IndexByFactor(double factor) {
		int current_x0_pt = (int)((modelList.size() - totalTime_sec *  1000f / getCollectorInterval_ms()) * factor);

		if(current_x0_pt<0)
			current_x0_pt = 0;

		return current_x0_pt;
	}

	public int calculateX1IndexByFactor(double factor) {

		int current_x1_pt = calculateX0IndexByFactor(factor) + (int)(totalTime_sec *  1000f / getCollectorInterval_ms());

		if(current_x1_pt>modelList.size()-1)
			current_x1_pt = modelList.size()-1;

		return (int)(current_x1_pt);
	}

	public int calculateXIndexByTime(double time) {
		int x = (int)(1000f / getCollectorInterval_ms() * time);
		if(x < 0)
			return 0;
		if(x > modelList.size()-1)
			return modelList.size()-1;
		return x;
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
		record.setValues(KeyFigureMetaData.MAV_SOURCE,_msg, meta);
	}


	private class CombinedConverter implements Runnable {

		int old_msg_hash = 0; long tms_start =0; long wait = 0; int old_mode=STOPPED;
		float perf = 0; AnalysisDataModel m = null;

		@Override
		public void run() {

			System.out.println("CombinedConverter started");
			mode = STOPPED;

			while(true) {

				if(!model.sys.isStatus(Status.MSP_CONNECTED)) {
					mode = STOPPED; old_mode = STOPPED;
					ulogger.enableLogging(false);
					LockSupport.parkNanos(2000000000);
				}

				if(!control.isSimulation())
					health.check(model);

				current.setValue("MAVGCLPERF", perf);

				if(mode!=STOPPED && old_mode == STOPPED && model.sys.isStatus(Status.MSP_CONNECTED)) {
					state.getRecordingProperty().set(READING_HEADER);
					ulogger.enableLogging(true);
					state.getLogLoadedProperty().set(false);
					state.getRecordingProperty().set(COLLECTING);
					tms_start = System.nanoTime() / 1000;
				}

				if(mode==STOPPED && old_mode != STOPPED) {
					ulogger.enableLogging(false);
					state.getRecordingProperty().set(STOPPED);
				}

				old_mode = mode;

				current.msg = null; wait = System.nanoTime();

				if(state.getCurrentUpToDate().getValue())
					current.setValues(KeyFigureMetaData.MSP_SOURCE,model,meta);


				if(ulogger.isLogging()) {
					//	record.setValues(KeyFigureMetaData.MSP_SOURCE,model,meta);
					record.setValues(KeyFigureMetaData.ULG_SOURCE,ulogger.getData(), meta);
					record.calculateVirtualKeyFigures(AnalysisDataModelMetaData.getInstance());
				}

				if(model.msg != null && model.msg.msg!=null && model.msg.msg.hashCode()!=old_msg_hash) {
					current.msg = model.msg;
					record.msg  = model.msg;
					old_msg_hash = model.msg.msg.hashCode();
				} else {
					current.msg = null; record.msg = null;
				}

				current.calculateVirtualKeyFigures(AnalysisDataModelMetaData.getInstance());


				if(mode!=STOPPED) {

					// Skip first
					if(!isFirst) {

						if(ulogger.isLogging())
							m = record.clone();
						else
							m = current.clone();
						m.tms = System.nanoTime() / 1000 - tms_start;
						m.dt_sec = m.tms / 1e6f;
						modelList.add(m);
					}

					try {
						for(ICollectorRecordingListener updater : listener)
							updater.update(System.nanoTime());
					} catch(Exception e) { }
					isFirst = false;
				}

				state.getRecordingAvailableProperty().set(modelList.size()>0);

				perf = (collector_interval_us*1000 - (System.nanoTime()-wait))/1e6f;
				LockSupport.parkNanos(collector_interval_us*1000 - (System.nanoTime()-wait) - 2000000);
			}
		}
	}

}
