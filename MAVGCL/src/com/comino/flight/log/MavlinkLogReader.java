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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import me.drton.jmavlib.log.px4.PX4LogReader;
import me.drton.jmavlib.log.ulog.ULogReader;

public class MavlinkLogReader implements IMAVLinkListener {

	private static final int LOG_PACKAG_DATA_LENGTH = 90;

	private static final int LOG_PACKAGES_SITL = 10;
	private static final int LOG_PACKAGES_PX4  = 10;

	private IMAVController control = null;
	private int     last_log_id   = 0;
	private long  log_bytes_read  = 0;
	private long  log_bytes_total = 0;

	private File tmpfile = null;
	private BufferedOutputStream out = null;

	private BooleanProperty   isCollecting = new SimpleBooleanProperty();
	private AnalysisModelService collector = AnalysisModelService.getInstance();

	private long received_ms=0;
	private long start = 0;
	private long time_utc=0;

	private boolean isUlog = true;

	private long package_count=0;
	private int package_size=0;

	private StateProperties state = null;
	private MSPLogger logger;
	private FileHandler fh;

	private ScheduledFuture<?> timeout;

	public MavlinkLogReader(IMAVController control) {
		this.control = control;
		this.control.addMAVLinkListener(this);
		this.state = StateProperties.getInstance();
		this.logger = MSPLogger.getInstance();
		this.fh     = FileHandler.getInstance();

		if(control.isSimulation()) {
			package_size = LOG_PACKAGES_SITL;
		}
		else
			package_size = LOG_PACKAGES_PX4;
	}

	public void requestLastLog() {

		try {
			this.tmpfile = FileHandler.getInstance().getTempFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
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
		logger.writeLocalMsg("[mgc] Request latest log");

		timeout = ExecutorService.get().scheduleAtFixedRate(() -> {
			if((System.currentTimeMillis()-received_ms)>15000)
				cancel("[mgc] Importing log failed: Timeout");
		}, 2000, 5000, TimeUnit.MILLISECONDS);
	}

	public void cancel() {
		cancel("[mgc] Loading log cancelled");
	}

	@Override
	public void received(Object o) {

		if( o instanceof msg_log_entry && isCollecting.get()) {

			msg_log_entry entry = (msg_log_entry) o;
			last_log_id = entry.num_logs - 1;

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
					time_utc = entry.time_utc;
					try {
						out = new BufferedOutputStream(new FileOutputStream(tmpfile));
					} catch (FileNotFoundException e) { cancel(); }
					log_bytes_read = 0; log_bytes_total = entry.size;

					if(entry.size==0) {
						cancel("[mgc] No import: LogSize zero");
						return;
					}

					logger.writeLocalMsg(
							"[mgc] Importing Log ("+last_log_id+") - "+(entry.size/1024)+" kb");
					package_count = 0;
					msg_log_request_data msg = new msg_log_request_data(255,1);
					msg.target_component = 1;
					msg.target_system = 1;
					msg.id = last_log_id;
					msg.ofs   = 0;
					msg.count = LOG_PACKAG_DATA_LENGTH * package_size;
					control.sendMAVLinkMessage(msg);
				}
			}
		}

		if( o instanceof msg_log_data && isCollecting.get()) {

			received_ms = System.currentTimeMillis();

			msg_log_data data = (msg_log_data) o;
			System.out.println(data.id+":"+log_bytes_total+"."+data.ofs+":"+data.count);
			
			try {
				for(int i=0;i< data.count;i++)
					out.write(data.data[i]);
			} catch (IOException e) { cancel(); return; }

			log_bytes_read = data.ofs;

			if(data.count<LOG_PACKAG_DATA_LENGTH) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) { return; }
				try {
					sendEndNotice();
					collector.clearModelList();
					if(isUlog) {
						ULogReader reader = new ULogReader(tmpfile.getAbsolutePath());
						UlogtoModelConverter converter = new UlogtoModelConverter(reader,collector.getModelList());
						converter.doConversion();
						reader.close();
					} else {
						PX4LogReader reader = new PX4LogReader(tmpfile.getAbsolutePath());
						PX4toModelConverter converter = new PX4toModelConverter(reader,collector.getModelList());
						converter.doConversion();
						reader.close();
					}
					long speed = log_bytes_total * 1000 / ( 1024 * (System.currentTimeMillis() - start));
					logger.writeLocalMsg("[mgc] Import completed ("+speed+" kb/sec)");
					state.getLogLoadedProperty().set(true);
					fh.setName("Log-"+last_log_id+"-"+time_utc);
				} catch (Exception e) {
					sendEndNotice();
					state.getLogLoadedProperty().set(false);
					logger.writeLocalMsg("[mgc] Importing log failed: "+e.getMessage());
					e.printStackTrace();
				}
				state.getLogLoadedProperty().set(true);
			} else {
				if(++package_count>=package_size) {
					package_count = 0;
					msg_log_request_data msg = new msg_log_request_data(255,1);
					msg.target_component = 1;
					msg.target_system = 1;
					msg.id = last_log_id;
					msg.ofs   = data.ofs+LOG_PACKAG_DATA_LENGTH;
					msg.count = LOG_PACKAG_DATA_LENGTH * package_size;
					control.sendMAVLinkMessage(msg);
					state.getProgressProperty().set((float)log_bytes_read / log_bytes_total);
				}
			}
		}
	}

	public BooleanProperty isCollecting() {
		return isCollecting;
	}

	private void cancel(String s) {
		if(!isCollecting.get())
			return;
		isCollecting.set(false);
		try {
			out.close();
		} catch (Exception e) {  }
		state.getProgressProperty().set(StateProperties.NO_PROGRESS);
		state.getLogLoadedProperty().set(false);
		logger.writeLocalMsg(s);
		killTimeOut();
	}


	private void sendEndNotice() {
		fh.setName("");
		isCollecting.set(false);
		state.getProgressProperty().set(StateProperties.NO_PROGRESS);
		msg_log_request_end msg = new msg_log_request_end(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
		killTimeOut();
	}

	private void killTimeOut() {
		if(timeout!=null)
			timeout.cancel(true);
	}

}
