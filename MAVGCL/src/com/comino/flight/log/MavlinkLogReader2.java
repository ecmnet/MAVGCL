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

import java.io.File;
import java.io.IOException;

import org.mavlink.messages.lquac.msg_log_data;
import org.mavlink.messages.lquac.msg_log_entry;
import org.mavlink.messages.lquac.msg_log_request_data;
import org.mavlink.messages.lquac.msg_log_request_list;

import com.comino.flight.file.FileHandler;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MavlinkLogReader2 implements IMAVLinkListener {

	private static final int LOG_PACKAG_DATA_LENGTH = 90;

	private IMAVController control = null;

	private int      last_log_id  = 0;

	private File tmpfile = null;

	private BooleanProperty   isCollecting    = new SimpleBooleanProperty();

	private final StateProperties state;
	private final MSPLogger logger;
	private final AnalysisModelService modelService;

	public MavlinkLogReader2(IMAVController control) {
		this.control = control;
		this.control.addMAVLinkListener(this);
		this.state = StateProperties.getInstance();
		this.logger = MSPLogger.getInstance();
		this.modelService = AnalysisModelService.getInstance();
	}

	public void requestLastLog() {

		try {
			this.tmpfile = FileHandler.getInstance().getTempFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		isCollecting.set(true);

		state.getProgressProperty().set(0);
		state.getLogLoadedProperty().set(false);

		logger.writeLocalMsg("[mgc] Request latest log");

		requestLogList(0);

	}

	public BooleanProperty isCollecting() {
		return isCollecting;
	}

	@Override
	public void received(Object o) {

		if( o instanceof msg_log_entry && isCollecting.get())
			handleLogEntry((msg_log_entry)o);

		if( o instanceof msg_log_data && isCollecting.get())
			handleLogData((msg_log_data) o);

	}

	private void handleLogEntry(msg_log_entry entry) {
		last_log_id = entry.num_logs - 1;

		if(last_log_id > -1) {
			if(entry.id != last_log_id)
				requestLogList(last_log_id);
			else {

				if(entry.size==0) {
					// TODO: Output Message
					return;
				}


				logger.writeLocalMsg("[mgc] Importing Log ("+last_log_id+") - "+(entry.size/1024)+" kb");
				requestDataPackages(0,entry.size);

			}
		}
	}

	private void handleLogData(msg_log_data data) {

	}

	private long searchNextMissingPackage() {

		return 0;
	}

	private void requestDataPackages(long offset, long len) {
		msg_log_request_data msg = new msg_log_request_data(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		msg.id = last_log_id;
		msg.ofs   = offset;
		msg.count = len;
		control.sendMAVLinkMessage(msg);
	}

	private void requestLogList(int id) {
		msg_log_request_list msg = new msg_log_request_list(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		msg.start= id;
		msg.end = id;
		control.sendMAVLinkMessage(msg);
	}


}
