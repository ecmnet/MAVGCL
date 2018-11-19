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

package com.comino.flight.log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
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
import com.comino.flight.parameter.MAVGCLPX4Parameters;
import com.comino.flight.parameter.ParameterAttributes;
import com.comino.mav.control.IMAVController;
import com.comino.msp.execution.control.listener.IMAVLinkListener;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import me.drton.jmavlib.log.px4.PX4LogReader;
import me.drton.jmavlib.log.ulog.ULogReader;

public class MavlinkLogReader implements IMAVLinkListener {

	private static final int LOG_PACKAG_DATA_LENGTH = 90;
	private static final int GET_LAST_LOG_ID = 99999;

	private static final int IDLE = 0;
	private static final int ENTRY = 1;
	private static final int DATA = 2;

	private int state = 0;

	private IMAVController control = null;

	private int last_log_id = 0;
	private long received_ms = 0;
	private int retry = 0;
	private long start = 0;
	private long time_utc = 0;

	private int total_package_count = 0;

	private int chunk_offset = 0;
	private int chunk_size = 0;

	private RandomAccessFile file = null;

	private BooleanProperty isCollecting = null;
	private List<Long> unread_packages = null;
	private ScheduledFuture<?> timeout;

	private String path = null;

	private final StateProperties props;
	private final MSPLogger logger;
	private final AnalysisModelService modelService;
	private final FileHandler fh;

	public MavlinkLogReader(IMAVController control) {
		this.control = control;
		this.props = StateProperties.getInstance();
		this.logger = MSPLogger.getInstance();
		this.fh = FileHandler.getInstance();
		this.modelService = AnalysisModelService.getInstance();
		this.isCollecting = new SimpleBooleanProperty();

		this.control.addMAVLinkListener(this);

	}

	public void requestLastLog() {
		try {
			this.path = fh.getTempFile().getPath();
			this.file = new RandomAccessFile(path, "rw");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		ParameterAttributes pp = MAVGCLPX4Parameters.getInstance().get("SDLOG_PROFILE");
		if(pp.value != 1 ) {
			logger.writeLocalMsg("[mgc] No import of extended logs. Use profile to '1'.");
			return;
		}


		isCollecting.set(true);
		state = ENTRY;
		retry = 0;

		modelService.getModelList().clear();

		props.getProgressProperty().set(0);
		props.getLogLoadedProperty().set(false);

		timeout = ExecutorService.get().scheduleAtFixedRate(() -> {
			if ((System.currentTimeMillis() - received_ms) > 2) {

				switch (state) {
				case IDLE:
					timeout.cancel(true);
					break;
				case ENTRY:
					if (++retry > 50) {
						abortReadingLog();
						return;
					}
					received_ms = System.currentTimeMillis();
					requestLogList(GET_LAST_LOG_ID);
					break;
				case DATA:
					if (++retry > 50) {
						abortReadingLog();
						return;
					}
					if (searchForNextUnreadPackage()) {
						requestDataPackages(chunk_offset * LOG_PACKAG_DATA_LENGTH, chunk_size * LOG_PACKAG_DATA_LENGTH);
					}
					received_ms = System.currentTimeMillis();
					break;
				}
			}
		}, 5000, 5, TimeUnit.MILLISECONDS);

		logger.writeLocalMsg("[mgc] Request latest log");
		start = System.currentTimeMillis();
		requestLogList(GET_LAST_LOG_ID);
	}

	public BooleanProperty isCollecting() {
		return isCollecting;
	}

	public void abortReadingLog() {
		stop();
		props.getLogLoadedProperty().set(false);
		props.getProgressProperty().set(StateProperties.NO_PROGRESS);
		logger.writeLocalMsg("[mgc] Abort reading log");
	}

	@Override
	public void received(Object o) {
		if (o instanceof msg_log_entry && isCollecting.get())
			handleLogEntry((msg_log_entry) o);

		if (o instanceof msg_log_data && isCollecting.get())
			handleLogData((msg_log_data) o);
	}

	private void handleLogEntry(msg_log_entry entry) {
		last_log_id = entry.num_logs - 1;
		received_ms = System.currentTimeMillis();
		if (last_log_id > -1) {
			if (entry.id != last_log_id)
				requestLogList(last_log_id);
			else {

				if (entry.size == 0) {
					stop();
					return;
				}
				time_utc = entry.time_utc;
				total_package_count = prepareUnreadPackageList(entry.size);
				System.out.println("Expected packages: " + unread_packages.size());
				logger.writeLocalMsg("[mgc] Importing Log (" + last_log_id + ") - " + (entry.size / 1024) + " kb");
				state = DATA;
				requestDataPackages(0, entry.size);

			}
		}
	}

	private void handleLogData(msg_log_data data) {
		received_ms = System.currentTimeMillis();
		retry = 0;

		int p = getPackageNumber(data.ofs);

		if (p >= unread_packages.size() || unread_packages.get(p) == -1)
			return;

		try {
			file.seek(data.ofs);
			for (int i = 0; i < data.count; i++)
				file.write((byte) (data.data[i] & 0x00FF));
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			return;
		}

	//	props.getLogLoadedProperty().set(true);
		fh.setName("in progress");

		unread_packages.set(p, (long) -1);
		// System.out.println("Package: "+p +" -> "+unread_packages.get(p));

		int unread_count = getUnreadPackageCount();
		props.getProgressProperty().set(1.0f - (float) unread_count / total_package_count);
		if (unread_count == 0) {
			stop();
			long speed = data.ofs * 1000 / (1024 * (System.currentTimeMillis() - start));
			sendEndNotice();
			try {
				ParameterAttributes pa = MAVGCLPX4Parameters.getInstance().get("SYS_LOGGER");
				if (pa == null || pa.value != 0) {

						ULogReader reader = new ULogReader(path);
						UlogtoModelConverter converter = new UlogtoModelConverter(reader, modelService.getModelList());
						converter.doConversion();
						reader.close();
				} else {
					PX4LogReader reader = new PX4LogReader(path);
					MAVGCLPX4Parameters.getInstance().setParametersFromLog(reader.getParameters());
					PX4toModelConverter converter = new PX4toModelConverter(reader, modelService.getModelList());
					converter.doConversion();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			logger.writeLocalMsg("[mgc] Import completed (" + speed + " kb/sec)");
			props.getLogLoadedProperty().set(true);
			DateFormat formatter = new SimpleDateFormat("YYYY-MM-DD");
			fh.setName("Log-" + last_log_id + "-" + formatter.format(time_utc));
			props.getProgressProperty().set(StateProperties.NO_PROGRESS);
		}
	}

	private void stop() {
		sendEndNotice();
		timeout.cancel(false);
		state = IDLE;
		isCollecting.set(false);
		try {
			file.close();
		} catch (IOException e) {
		}
	}

	private boolean searchForNextUnreadPackage() {
		chunk_offset = -1;
		for (int i = 0; i < unread_packages.size() && chunk_offset == -1; i++) {
			if (unread_packages.get(i) != -1) {
				chunk_offset = i;
				chunk_size = unread_packages.size();
			}
		}

		if (chunk_offset == -1)
			return false;

		chunk_size = 0;
		for (int i = chunk_offset; i < unread_packages.size() && unread_packages.get(i) != -1; i++)
			chunk_size++;

		return true;
	}

	private int getUnreadPackageCount() {
		int c = 0;
		for (int i = 0; i < unread_packages.size(); i++) {
			if (unread_packages.get(i) != -1)
				c++;
		}
		return c;
	}

	private int prepareUnreadPackageList(long size) {
		unread_packages = new ArrayList<Long>();
		// TODO determine count of packages and fill list with offset
		int count = getPackageNumber(size);
		for (long i = 0; i < count + 1; i++)
			unread_packages.add(i * LOG_PACKAG_DATA_LENGTH);
		return count;
	}

	private int getPackageNumber(long offset) {
		return (int) (offset / LOG_PACKAG_DATA_LENGTH);
	}

	private void requestDataPackages(long offset, long len) {
		// System.out.println("Request packages from: "+offset+ " ("+len+"
		// bytes)");
		msg_log_request_data msg = new msg_log_request_data(255, 1);
		msg.target_component = 1;
		msg.target_system = 1;
		msg.id = last_log_id;
		msg.ofs = offset;
		msg.count = len;
		control.sendMAVLinkMessage(msg);
	}

	private void requestLogList(int id) {
		msg_log_request_list msg = new msg_log_request_list(255, 1);
		msg.target_component = 1;
		msg.target_system = 1;
		msg.start = id;
		msg.end = id;
		control.sendMAVLinkMessage(msg);
	}

	private void sendEndNotice() {
		msg_log_request_end msg = new msg_log_request_end(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
	}

}
