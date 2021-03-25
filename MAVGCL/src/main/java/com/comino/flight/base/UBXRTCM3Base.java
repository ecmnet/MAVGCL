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

package com.comino.flight.base;


import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_gps_rtcm_data;

import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavbase.ublox.reader.StreamEventListener;
import com.comino.mavbase.ublox.reader.UBXSerialConnection;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.model.segment.GPS;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavutils.legacy.ExecutorService;
import com.comino.mavutils.workqueue.WorkQueue;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class UBXRTCM3Base implements Runnable {

	private static UBXRTCM3Base instance = null;
	private UBXSerialConnection ubx = null;

	private AnalysisModelService analysisModelService = null;

	private BooleanProperty svin  	= new SimpleBooleanProperty(false);
	private BooleanProperty valid 	= new SimpleBooleanProperty(false);
	private BooleanProperty current = new SimpleBooleanProperty(false);

	private GPS base = null;
	private Status status = null;

	private boolean connected = false;

	private float mean_acc = 0;
	private DecimalFormat format = new DecimalFormat("#0.0");

	private MSPLogger logger =null;

	private IMAVController control;
	
	private final WorkQueue wq = WorkQueue.getInstance();
	private int ubxtask = 0;

	public static UBXRTCM3Base getInstance(IMAVController control, AnalysisModelService analysisModelService) {
		if(instance == null) {
			instance = new UBXRTCM3Base(control, analysisModelService);
		}
		return instance;
	}

	public static UBXRTCM3Base getInstance() {
		return instance;
	}

	public UBXRTCM3Base(IMAVController control, AnalysisModelService analysisModelService) {

		this.control = control;
		this.analysisModelService = analysisModelService;
		this.logger = MSPLogger.getInstance();

		base = control.getCurrentModel().base;
		status = control.getCurrentModel().sys;

		ubxtask = wq.addCyclicTask("LP", 5000, this);

		svin.addListener((p,o,n) -> {
			if(n.booleanValue())
				logger.writeLocalMsg("[mgc] Survey-In started", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
		});

		valid.addListener((p,o,n) -> {
			if(n.booleanValue()) {
				logger.writeLocalMsg("[mgc] RTCM3 stream active", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
			}
			else
				logger.writeLocalMsg("[mgc] RTCM3 base lost", MAV_SEVERITY.MAV_SEVERITY_WARNING);
		});

	}

	public double getLongitude() {
		return base.longitude;
	}

	public double getLatitude() {
		return base.latitude;
	}

	public BooleanProperty getSVINStatus() {
		return svin;
	}

	public BooleanProperty getValidStatus() {
		return valid;
	}

	public BooleanProperty getCurrentLocationProperty() {
		return current;
	}

	public float getBaseAccuracy() {
		return mean_acc;
	}

	public boolean isConnected() {
		return connected;
	}

	public int getBaseNumSat() {
		return (int)base.numsat;
	}

	@Override
	public void run() {

		if(connected)
			return;

		this.ubx = new UBXSerialConnection(9600);
		this.ubx.setMeasurementRate(1);

		try {
			float accuracy = Float.parseFloat(MAVPreferences.getInstance().get(MAVPreferences.RTKSVINACC, "3.0"));
			int time = Integer.parseInt(MAVPreferences.getInstance().get(MAVPreferences.RTKSVINTIM, "60"));
			if(!ubx.init(time,accuracy)) {
				wq.removeTask("LP", ubxtask);
				logger.writeLocalMsg("[mgc] USB port in use. Searching for base stopped.", MAV_SEVERITY.MAV_SEVERITY_DEBUG);
			}
			connected = true;
		} catch (Exception e) {
			return;
		}

		ubx.addStreamEventListener( new StreamEventListener() {

			@Override
			public void streamClosed() {
				try {
					if(svin.get())
						logger.writeLocalMsg("[mgc] Survey-In timeout", MAV_SEVERITY.MAV_SEVERITY_WARNING);
					connected = false;
					valid.set(false); svin.set(false); current.set(false);
					analysisModelService.getCurrent().setValue("BASENO",  Double.NaN);
					analysisModelService.getCurrent().setValue("BASELAT", Double.NaN);
					analysisModelService.getCurrent().setValue("BASELON", Double.NaN);
					ubx.release(false, 100);
				} catch (Exception e) {
					return;
				}
			}

			@Override
			public void getSurveyIn(float time_svin, boolean is_svin, boolean is_valid, float meanacc) {
				svin.set(is_svin);
				mean_acc = meanacc;
				if((time_svin % 30) == 0 && is_svin) {
					current.set(mean_acc < 20.0f);
					logger.writeLocalMsg("[mgc] Survey-In: "+format.format(meanacc)+"m ["+(int)base.numsat+"]", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				}
				analysisModelService.getCurrent().setValue("SVINACC", meanacc);
			}


			@Override
			public void getPosition(double lat, double lon, double altitude, int fix, int sats) {
				base.latitude   = (float)lat;
				base.longitude  = (float)lon;
				base.altitude   = (short)altitude;
				base.numsat     = sats;
				if(!control.isConnected() && !StateProperties.getInstance().getLogLoadedProperty().get()) {
					analysisModelService.getCurrent().setValue("BASENO", sats);
					analysisModelService.getCurrent().setValue("BASELAT", lat);
					analysisModelService.getCurrent().setValue("BASELON", lon);
				}
		//		System.out.println("Base position: Lat: "+lat+" Lon: "+lon+ " Alt: "+altitude+" Sat: "+sats+" Acc: "+mean_acc);
			}

			@Override
			public void getRTCM3(byte[] buffer, int len) {

				if(!control.isConnected() || !status.isSensorAvailable(Status.MSP_GPS_AVAILABILITY) || svin.get())
					return;

				valid.set(true); current.set(true);

				msg_gps_rtcm_data msg = new msg_gps_rtcm_data(2,1);
				if(len < msg.data.length) {
					msg.flags = 0;
					msg.len   = len;
					for(int i = 0;i<len;i++)
						msg.data[i] = buffer[i];
					control.sendMAVLinkMessage(msg);
					//			System.out.println(msg);
				} else {
					int start = 0;
					while (start < len) {
						int length = Math.min(len - start, msg.data.length);
						msg.flags = 1;
						msg.len   = length;
						for(int i = start;i<length;i++)
							msg.data[i] = buffer[i];
						control.sendMAVLinkMessage(msg);
						start += length;
					}
				}

			}
		});
	}
}
