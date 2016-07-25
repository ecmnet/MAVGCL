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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.observables.StateProperties;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

public class AnalysisModelService {

	private static AnalysisModelService instance = null;

	public static  final int STOPPED		 	= 0;
	public static  final int PRE_COLLECTING 	= 1;
	public static  final int COLLECTING     	= 2;
	public static  final int POST_COLLECTING    = 3;

	private static final int MODELCOLLECTOR_INTERVAL_US = 50000;

	private DataModel								model       = null;
	private AnalysisDataModel				    	current     = null;
	private ArrayList<AnalysisDataModel> 		    modelList   = null;

	private int     mode = 0;

	private  int  totalTime_sec = 30;

	public static AnalysisModelService getInstance(DataModel model) {
		if(instance==null)
			instance = new AnalysisModelService(model);
		return instance;
	}

	public static AnalysisModelService getInstance() {
		return instance;
	}


	private AnalysisModelService(DataModel model) {
		this.modelList     = new ArrayList<AnalysisDataModel>();
		this.model         = model;
		this.current       =  new AnalysisDataModel();
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


			new Thread(new Collector(0)).start();
		}
		return mode != STOPPED;
		//service = ExecutorService.get().scheduleAtFixedRate(new Collector(), 0, MODELCOLLECTOR_INTERVAL_US, TimeUnit.MICROSECONDS);
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
		modelList.addAll(list);
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
		int current_x0_pt = (int)(
				( modelList.size()
						- totalTime_sec *  1000f
						/ getCollectorInterval_ms())
				* factor);

		if(current_x0_pt<0)
			current_x0_pt = 0;

		return current_x0_pt;
	}

	public int calculateX1Index(double factor) {

		int current_x1_pt = calculateX0Index(factor) +
				(int)(totalTime_sec *  1000f
						/ getCollectorInterval_ms());

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

	private class Converter implements Runnable {

		@Override
		public void run() {
			long tms = 0;
			while(true) {
				current.msg = null;
				current.setValuesMSP(model, AnalysisDataModelMetaData.getInstance());
				if(model.msg != null && model.msg.tms > tms) {
					current.msg = model.msg;
					tms = current.msg.tms+100;
				} else
					current.msg = null;
				LockSupport.parkNanos(MODELCOLLECTOR_INTERVAL_US*1000);
			}
		}
	}


	private class Collector implements Runnable {

		int pre_delay_count=0; int count = 0;

		public Collector(int pre_delay_sec) {
			if(pre_delay_sec>0) {
				mode = PRE_COLLECTING;
				this.pre_delay_count = pre_delay_sec * 1000000 / MODELCOLLECTOR_INTERVAL_US;
			}
		}

		@Override
		public void run() {
			long tms = System.nanoTime() / 1000;
			StateProperties.getInstance().getLogLoadedProperty().set(false);
			while(mode!=STOPPED) {
				synchronized(this) {
					AnalysisDataModel m = current.clone();
					m.tms = System.nanoTime() / 1000 - tms;
					modelList.add(m);
					count++;
				}
				LockSupport.parkNanos(MODELCOLLECTOR_INTERVAL_US*1000);

				if(mode==PRE_COLLECTING) {
					int _delcount = count - pre_delay_count;
					if(_delcount > 0) {
						for(int i = 0; i < _delcount; i++ )
							modelList.remove(0);
					}

				}
			}
		}

	}

}
