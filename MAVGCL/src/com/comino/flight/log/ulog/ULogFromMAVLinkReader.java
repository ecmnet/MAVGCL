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

package com.comino.flight.log.ulog;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_logging_ack;
import org.mavlink.messages.lquac.msg_logging_data;
import org.mavlink.messages.lquac.msg_logging_data_acked;

import com.comino.flight.prefs.MAVPreferences;
import com.comino.jmavlib.extensions.UlogMAVLinkParser;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.utils.ExecutorService;


public class ULogFromMAVLinkReader implements IMAVLinkListener {


	private final int STATE_HEADER_IDLE				= 0;
	private final int STATE_HEADER_WAIT				= 1;
	private final int STATE_DATA            		= 2;

	private IMAVController control   = null;
	private int state = STATE_HEADER_IDLE;
	private UlogMAVLinkParser parser = null;

	private int header_processed = 0;
	private int data_processed = 0;
	private int package_lost=0;

	private boolean debug = false;

	private MSPLogger logger = null;


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

	public void enableLogging(boolean enable) {

		state=STATE_HEADER_IDLE;

		if(!MAVPreferences.getInstance().getBoolean(MAVPreferences.ULOGGER, false) && !debug) {
			if(enable)
				logger.writeLocalMsg("[mgc] Logging via MAVLink streaming",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
			return;
		}

		long tms = System.currentTimeMillis();

		if(enable)  {
			parser.reset();
			logger.writeLocalMsg("[mgc] Try to start ULog streaming",MAV_SEVERITY.MAV_SEVERITY_DEBUG);
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_START,0);

			while(state!=STATE_DATA ) {
				LockSupport.parkNanos(10000000);
				if((System.currentTimeMillis()-tms)>5000) {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
					logger.writeLocalMsg("[mgc] Logging via MAVLink streaming",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
					state=STATE_HEADER_IDLE;
					return;
				}
			}

			ExecutorService.get().schedule(() -> {
				if(state==STATE_DATA)
					logger.writeLocalMsg("[mgc] ULog lost package ratio: "+(int)(lostPackageRatio()*100f)+"%",
							MAV_SEVERITY.MAV_SEVERITY_NOTICE);
			}, 3, TimeUnit.SECONDS);

			logger.writeLocalMsg("[mgc] Logging via ULog streaming",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
		} else {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
			if(state==STATE_DATA) {
				state=STATE_HEADER_IDLE;
			}
		}
	}

	public boolean isLogging() {
		return state==STATE_DATA;
	}

	@Override
	public  void received(Object o) {

		if( o instanceof msg_logging_data_acked) {

			msg_logging_data_acked log = (msg_logging_data_acked)o;
			msg_logging_ack ack = new msg_logging_ack(255,1);
			ack.target_component=1;
			ack.target_system=1;
			ack.isValid = true;
			ack.sequence = log.sequence;
			control.sendMAVLinkMessage(ack);

			parser.addToBuffer(log);

			if(state==STATE_HEADER_IDLE) {
				if(parser.checkHeader()) {
					header_processed = log.sequence;
					state = STATE_HEADER_WAIT;
					System.out.println("Start reading header");
				} else
					return;
			}

			if(state==STATE_DATA) {
				parser.parseHeader();
				header_processed++;
				return;
			}

			if(header_processed != log.sequence) {
				logger.writeLocalMsg("[mgc] Fallback to MAVLink logging: "+header_processed+":"+log.sequence,MAV_SEVERITY.MAV_SEVERITY_DEBUG);
				System.err.println("Header sequence error:"+header_processed+":"+log.sequence);
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
				header_processed = 0;
				state=STATE_HEADER_IDLE;
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
				} else
				   parser.addToBuffer(log, true);
				parser.parseData(debug);
			}

			if(++data_processed > 65535)
				data_processed = 0;
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
