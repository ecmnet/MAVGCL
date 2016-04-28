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


package com.comino.flight.px4log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_log_data;
import org.mavlink.messages.lquac.msg_log_entry;
import org.mavlink.messages.lquac.msg_log_request_data;
import org.mavlink.messages.lquac.msg_log_request_end;
import org.mavlink.messages.lquac.msg_log_request_list;

import com.comino.flight.widgets.statusline.StatusLineWidget;
import com.comino.mav.control.IMAVController;
import com.comino.model.file.FileHandler;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.model.DataModel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import me.drton.jmavlib.log.px4.PX4LogReader;

public class MAVPX4LogReader implements IMAVLinkListener {

	private IMAVController control = null;
	private int     last_log_id   = 0;
	private long  log_bytes_read  = 0;
	private long  log_bytes_total = 0;

	private File tmpfile = null;
	private BufferedOutputStream out = null;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();

	private long tms = 0;
	private long time_utc=0;

	public MAVPX4LogReader(IMAVController control) {
		this.control = control;
		this.control.addMAVLinkListener(this);

		try {
			this.tmpfile = FileHandler.getInstance().getTempFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void requestLastLog() {
		control.getCollector().clearModelList();
		isCollecting.set(true);
		msg_log_request_list msg = new msg_log_request_list(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
	}

	public void cancel() {

		if(!isCollecting.get())
			return;

		try {
			out.close();
		} catch (IOException e) { e.printStackTrace();  }

		control.getCollector().clearModelList();
		isCollecting.set(false);
		StatusLineWidget.showProgressIndicator(false);
		msg_log_request_end msg = new msg_log_request_end(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
		MSPLogger.getInstance().writeLocalMsg("Import from device cancelled");
	}

	@Override
	public void received(Object o) {

		if( o instanceof msg_log_entry) {

			if(!isCollecting.get())
				return;

			msg_log_entry entry = (msg_log_entry) o;
			last_log_id = entry.num_logs - 1;

			if(last_log_id > -1) {
				if(entry.id != last_log_id) {
					StatusLineWidget.showProgressIndicator(true);
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
					MSPLogger.getInstance().writeLocalMsg(
							"Loading px4log from device ("+last_log_id+") - Size: "+(entry.size/1024)+" kb");
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

			for(int i=0;i< data.count;i++) {
				try {
					out.write(data.data[i]);
				} catch (IOException e) { cancel(); }
			}
			log_bytes_read = data.ofs;

			if((System.currentTimeMillis()-tms)>5000) {
				MSPLogger.getInstance().writeLocalMsg("Loading px4log from device: "+getProgress()+"%",
						MAV_SEVERITY.MAV_SEVERITY_DEBUG);
				tms = System.currentTimeMillis();
			}

			if(data.count < 90) {
				try {
					out.close();
				} catch (IOException e) { cancel();  }
				try {

					msg_log_request_end msg = new msg_log_request_end(255,1);
					msg.target_component = 1;
					msg.target_system = 1;
					control.sendMAVLinkMessage(msg);


					ArrayList<DataModel>modelList = new ArrayList<DataModel>();
					PX4LogReader reader = new PX4LogReader(tmpfile.getAbsolutePath());
					PX4toModelConverter converter = new PX4toModelConverter(reader,modelList);
					converter.doConversion();
					control.getCollector().setModelList(modelList);
					MSPLogger.getInstance().writeLocalMsg("Reading log from device finished");
				} catch (Exception e) { e.printStackTrace(); }

				FileHandler.getInstance().setName("PX4Log-"+last_log_id+"-"+time_utc);
				StatusLineWidget.showProgressIndicator(false);

				isCollecting.set(false);
			}
		}
	}

	public int getProgress() {
		return (int)((log_bytes_read * 100) / log_bytes_total);
	}

	public BooleanProperty isCollecting() {
		return isCollecting;
	}

}
