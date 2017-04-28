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


package com.comino.flight.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_log_data;
import org.mavlink.messages.lquac.msg_log_entry;
import org.mavlink.messages.lquac.msg_log_request_data;
import org.mavlink.messages.lquac.msg_log_request_end;
import org.mavlink.messages.lquac.msg_log_request_list;

import com.comino.flight.file.FileHandler;
import com.comino.flight.log.px4log.PX4toModelConverter;
import com.comino.flight.log.ulog.UlogtoModelConverter;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.parameter.PX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import me.drton.jmavlib.log.px4.PX4LogReader;
import me.drton.jmavlib.log.ulog.ULogReader;

public class MavlinkLogReader implements IMAVLinkListener {

	private IMAVController control = null;
	private int     last_log_id   = 0;
	private long  log_bytes_read  = 0;
	private long  log_bytes_total = 0;

	private File tmpfile = null;
	private BufferedOutputStream out = null;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private AnalysisModelService collector = AnalysisModelService.getInstance();

	private long tms = 0;
	private long start = 0;
	private long time_utc=0;

	private ScheduledFuture<?> timeout = null;
	private StateProperties state = null;
	private MSPLogger logger;
	private FileHandler fh;

	public MavlinkLogReader(IMAVController control) {
		this.control = control;
		this.control.addMAVLinkListener(this);
		this.state = StateProperties.getInstance();
		this.logger = MSPLogger.getInstance();
		this.fh     = FileHandler.getInstance();
	}

	public void requestLastLog() {

		try {
			this.tmpfile = FileHandler.getInstance().getTempFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log_bytes_read = 0; log_bytes_total = 0;
		start = System.currentTimeMillis();
		isCollecting.set(true);
		msg_log_request_list msg = new msg_log_request_list(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
		state.getProgressProperty().set(0);
		state.getLogLoadedProperty().set(false);
		fh.setName("Log loading..");
		logger.writeLocalMsg("[mgc] Request MAVLinkLog");
		setTimeout();
	}

	public void cancel() {

		if(!timeout.cancel(true))
			System.out.println("LOG Worker thread cancelling failed");

		if(!isCollecting.get())
			return;

		try {
			out.close();
		} catch (Exception e) {  }
		sendEndNotice();
		state.getLogLoadedProperty().set(false);
		logger.writeLocalMsg("[mgc] Loading MAVLinkLog cancelled");
	}

	@Override
	public void received(Object o) {


		if( o instanceof msg_log_entry) {

			if(!isCollecting.get())
				return;

			msg_log_entry entry = (msg_log_entry) o;
			last_log_id = entry.num_logs - 1;
			setTimeout();

			if(last_log_id > -1) {
				if(entry.id != last_log_id) {
					msg_log_request_list msg = new msg_log_request_list(255,1);
					msg.target_component = 1;
					msg.target_system = 1;
					msg.start= last_log_id;
					msg.end = last_log_id;
					control.sendMAVLinkMessage(msg);
				}
				else {
					timeout.cancel(true);
					time_utc = entry.time_utc;
					try {
						out = new BufferedOutputStream(new FileOutputStream(tmpfile));
					} catch (FileNotFoundException e) { cancel(); }
					log_bytes_read = 0; log_bytes_total = entry.size;
					if(log_bytes_total==0) {
						logger.writeLocalMsg("[mgc] Loading log failed: Timeout");
						cancel();
						return;
					}
					logger.writeLocalMsg(
							"[mgc] Loading Log ("+last_log_id+") - Size: "+(entry.size/1024)+" kb");
					msg_log_request_data msg = new msg_log_request_data(255,1);
					msg.target_component = 1;
					msg.target_system = 1;
					msg.id = last_log_id;
					msg.count = Long.MAX_VALUE;
					control.sendMAVLinkMessage(msg);
				}
			}
		}

		if( o instanceof msg_log_data) {

			if(!isCollecting.get())
				return;

			msg_log_data data = (msg_log_data) o;
		//	System.out.println(data.id+":"+log_bytes_total+"."+data.ofs);
			for(int i=0;i< data.count;i++) {
				try {
					out.write(data.data[i]);
					out.flush();
				} catch (IOException e) { cancel(); }
			}
			log_bytes_read = data.ofs;

			if((System.currentTimeMillis()-tms)>5000) {
				state.getProgressProperty().set(getProgress()/100f);
				tms = System.currentTimeMillis();
			}

			if(data.count==0) {
				try {
					System.out.println();
					out.flush();
					out.close();
				} catch (IOException e) { cancel();  }
				try {
					collector.clearModelList();
					sendEndNotice();
					timeout.cancel(true);
					Thread.sleep(100);
					if(PX4Parameters.getInstance().get("SYS_LOGGER").value==1) {
						PX4LogReader reader = new PX4LogReader(tmpfile.getAbsolutePath());
						PX4toModelConverter converter = new PX4toModelConverter(reader,collector.getModelList());
						converter.doConversion();
						reader.close();
					} else {
						ULogReader reader = new ULogReader(tmpfile.getAbsolutePath());
						UlogtoModelConverter converter = new UlogtoModelConverter(reader,collector.getModelList());
						converter.doConversion();
						reader.close();
					}
					long speed = log_bytes_total * 1000 / ( 1024 * (System.currentTimeMillis() - start));
					logger.writeLocalMsg("[mgc] Reading log finished ("+speed+" kbtyes/sec)");
					state.getLogLoadedProperty().set(true);
					fh.setName("Log-"+last_log_id+"-"+time_utc);
				} catch (Exception e) {
					sendEndNotice();
					state.getLogLoadedProperty().set(false);
					logger.writeLocalMsg("[mgc] Loading log failed: "+e.getMessage());
					e.printStackTrace();
				}
				state.getLogLoadedProperty().set(true);
			} else
				setTimeout();
		}
	}

	public int getProgress() {
		return (int)((log_bytes_read * 100) / log_bytes_total);
	}

	public BooleanProperty isCollecting() {
		return isCollecting;
	}


	private void setTimeout() {
		if(timeout!=null)
			timeout.cancel(true);
		timeout = ExecutorService.get().schedule(() -> {
			try {
				out.close();
			} catch (IOException e) { }
			sendEndNotice();
			logger.writeLocalMsg("[mgc] Loading log failed: Timeout");
			state.getLogLoadedProperty().set(false);
		}, 2, TimeUnit.SECONDS);
	}

	private void sendEndNotice() {
		fh.setName("");
		isCollecting.set(false);
		state.getProgressProperty().set(-1);
		msg_log_request_end msg = new msg_log_request_end(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
	}

}
