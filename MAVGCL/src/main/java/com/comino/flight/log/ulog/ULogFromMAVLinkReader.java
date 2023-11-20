/****************************************************************************
 *
 *   Copyright (c) 2017,2021 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.log.ulog;

import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_logging_ack;
import org.mavlink.messages.lquac.msg_logging_data;
import org.mavlink.messages.lquac.msg_logging_data_acked;

import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.jmavlib.extensions.UlogMAVLinkParser;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.mavlink.IMAVLinkListener;
import com.comino.mavutils.workqueue.WorkQueue;


public class ULogFromMAVLinkReader implements IMAVLinkListener {


	private final int STATE_HEADER_IDLE				= 0;
	private final int STATE_HEADER_WAIT				= 1;
	private final int STATE_DATA            			= 2;

	private IMAVController control   = null;
	private int state = STATE_HEADER_IDLE;
	private UlogMAVLinkParser parser = null;

	private int header_processed = 0;
	private int data_processed = 0;
	private int package_lost=0;

	private boolean debug = false;
	private AnalysisModelService service = null;


	private MSPLogger logger = null;
	
	private final WorkQueue wq = WorkQueue.getInstance();


	public ULogFromMAVLinkReader(IMAVController control, boolean debug)  {
		this.parser = new UlogMAVLinkParser();
		this.control = control;
		this.control.addMAVLinkListener(this);
		this.logger = MSPLogger.getInstance();
		this.debug  = debug;
	}

	public ULogFromMAVLinkReader(IMAVController control)  {
		this(control, false);
	}

	public Map<String, Object> getData() {
		return parser.getDataBuffer();
	}

	public Map<String,String> getFieldList() {
		return parser.getFieldList();
	}

	public boolean enableLogging(boolean enable) {
		
		this.service = AnalysisModelService.getInstance();

	//	new Thread(() -> {

			state=STATE_HEADER_IDLE;

			if(!control.isConnected())
				return false;
			

			if(!MAVPreferences.getInstance().getBoolean(MAVPreferences.ULOGGER, false) && !debug) {
				if(enable) {
					service.setCollectorInterval(AnalysisModelService.DEFAULT_INTERVAL_US);
					logger.writeLocalMsg("[mgc] Logging via MAVLink streaming",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				}
				return false;
			}

			if(enable)  {
				service.setCollectorInterval(AnalysisModelService.MAVHIRES_INTERVAL_US);
				long tms = System.currentTimeMillis();
				parser.reset(); header_processed = 0; package_lost = 0;
				logger.writeLocalMsg("[mgc] Try to start ULog streaming",MAV_SEVERITY.MAV_SEVERITY_DEBUG);
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_START,0);

				while(state!=STATE_DATA ) {
					// TODO: This is blocking the UI Thread -> resolve
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {	}

					if((System.currentTimeMillis()-tms)>12000) {
						service.setCollectorInterval(AnalysisModelService.DEFAULT_INTERVAL_US);
						control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
						logger.writeLocalMsg("[mgc] Logging via MAVLink streaming",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
						state=STATE_HEADER_IDLE;
						return false;
					}
				}

				wq.addSingleTask("LP",5000,() -> {
					if(state==STATE_DATA && lostPackageRatio() > 0.02f)
						logger.writeLocalMsg("[mgc] ULog lost package ratio: "+(int)(lostPackageRatio()*100f)+"%",
								MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				});

	
				logger.writeLocalMsg("[mgc] Logging via ULog streaming",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
			} else {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
				if(state==STATE_DATA) {
					state=STATE_HEADER_IDLE;
				}
				return false;
			}
			return true;
//		}).start();
	}

	public boolean isReadingHeader() {
		return state==STATE_HEADER_WAIT;
	}

	public boolean isLogging() {
		return state==STATE_DATA;
	}

	@Override
	public  void received(Object o) {

		if( o instanceof msg_logging_data_acked) {

			msg_logging_data_acked log = (msg_logging_data_acked)o;

			if(state==STATE_DATA) {
				parser.parseHeader();
				header_processed++;
				msg_logging_ack ack = new msg_logging_ack(255,1);
				ack.target_component=1;
				ack.target_system=1;
				ack.isValid = true;
				ack.sequence = log.sequence;
				control.sendMAVLinkMessage(ack);
				return;
			}

			if(header_processed != log.sequence) {
				return;
			}
			parser.addToBuffer(log);
			msg_logging_ack ack = new msg_logging_ack(255,1);
			ack.target_component=1;
			ack.target_system=1;
			ack.isValid = true;
			ack.sequence = log.sequence;
			control.sendMAVLinkMessage(ack);

			if(state==STATE_HEADER_IDLE) {
				if(parser.checkHeader()) {
					header_processed = log.sequence;
					state = STATE_HEADER_WAIT;
					System.out.println("ULOG Start reading header");
				} else
					return;
			}

			parser.parseHeader();
			header_processed++;
		}

		if( o instanceof msg_logging_data) {

			msg_logging_data log = (msg_logging_data)o;

			if(state==STATE_HEADER_IDLE) {
				parser.reset();
				data_processed = 0;
				package_lost=0;
				return;
			}

			if(state==STATE_HEADER_WAIT) {
				System.out.println("ULOG build subscriptions");
				parser.buildSubscriptions();
				data_processed = header_processed;
				parser.clearBuffer();
				state = STATE_DATA;
			}

			if(state==STATE_DATA) {
				if(data_processed != log.sequence) {
					data_processed = log.sequence;
					package_lost++;
					parser.addToBuffer(log, false);
				} else {
					parser.addToBuffer(log, true);
				}
				parser.parseData(debug);
			}

			if(++data_processed > 65535) {
				data_processed = 0;
				package_lost = 0;
			}
		}
	}

	public int getHeaderProcessed() {
		return header_processed;
	}

	public int getDataPackagesProcessed() {
		return data_processed;
	}

	public float lostPackageRatio() {
		if(data_processed == 0)
			return Float.NaN;
		else
			return (float)package_lost/data_processed;
	}

	//  helpers for dev
	//	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	//	public static String bytesToHex(byte[] bytes, int len) {
	//		char[] hexChars = new char[len * 2];
	//		for ( int j = 0; j <len; j++ ) {
	//			int v = bytes[j] & 0xFF;
	//			hexChars[j * 2] = hexArray[v >>> 4];
	//			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	//		}
	//		return new String(hexChars);
	//	}
	//
	//	public static String intsToHex(int[] bytes, int len) {
	//		char[] hexChars = new char[len * 2];
	//		for ( int j = 0; j <len; j++ ) {
	//			int v = bytes[j] & 0xFF;
	//			hexChars[j * 2] = hexArray[v >>> 4];
	//			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	//		}
	//		return new String(hexChars);
	//	}

}
