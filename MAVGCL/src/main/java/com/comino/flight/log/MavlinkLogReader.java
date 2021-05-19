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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.prefs.Preferences;

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
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.mavlink.IMAVLinkListener;
import com.comino.mavcom.param.ParameterAttributes;
import com.comino.mavutils.legacy.ExecutorService;
import com.comino.mavutils.workqueue.WorkQueue;

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
	private int retry = 0;
	private long start = 0;
	private long time_utc = 0;

	private int total_package_count = 0;

	private volatile int chunk_offset = 0;
	private volatile int chunk_size = 0;

	private int read_count = 0;

	private RandomAccessFile file = null;

	private BooleanProperty isCollecting = null;
	private List<Long> unread_packages = null;
	private int timeout;

	private String path = null;

	private final StateProperties props;
	private final MSPLogger logger;
	private final AnalysisModelService modelService;
	private final FileHandler fh;
	private final Preferences userPrefs;
	private int speed=0;

	private final WorkQueue wq = WorkQueue.getInstance();

	public MavlinkLogReader(IMAVController control) {
		this.control = control;
		this.props = StateProperties.getInstance();
		this.userPrefs = MAVPreferences.getInstance();
		this.logger = MSPLogger.getInstance();
		this.fh = FileHandler.getInstance();
		this.modelService = AnalysisModelService.getInstance();
		this.isCollecting = new SimpleBooleanProperty();
		this.control.addMAVLinkListener(this);

		props.getRecordingProperty().addListener((o,ov,nv) -> {
			if(nv.intValue() != AnalysisModelService.STOPPED && isCollecting.get())
				abortReadingLog();
		});	
	}

	public void requestLastLog() {

		if(props.getRecordingProperty().intValue()!=AnalysisModelService.STOPPED)
			return;

		try {
			this.path = fh.getTempFile().getPath();
			this.file = new RandomAccessFile(path, "rw");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		ParameterAttributes pp = MAVGCLPX4Parameters.getInstance().get("SDLOG_PROFILE");
		if(pp!=null) {
			if(pp.value != 1  && pp.value < 17 ) {
				logger.writeLocalMsg("[mgc] No import of extended logs. Use profile to '1' or '17'.");
				return;
			}
		}



		isCollecting.set(true);
		state = ENTRY;
		retry = 0;
		read_count = 0;


		props.getProgressProperty().set(0);
		props.getLogLoadedProperty().set(false);

		timeout = wq.addCyclicTask("LP",5,() -> {

			switch (state) {
			case IDLE:
				wq.removeTask("LP",timeout);
				retry=0;
				read_count = 0;
				break;
			case ENTRY:
				if (++retry > 500) {
					abortReadingLog();
					return;
				}
				requestLogList(GET_LAST_LOG_ID);
				break;
			case DATA:
				if (++retry > 200) {
					abortReadingLog();
					return;
				}
				if(searchForNextUnreadPackage()) 
					requestDataPackages(chunk_offset * LOG_PACKAG_DATA_LENGTH, chunk_size  * LOG_PACKAG_DATA_LENGTH);
				break;
			}
		});

		start = System.currentTimeMillis();
		requestLogList(GET_LAST_LOG_ID);
	}

	public BooleanProperty isCollecting() {
		return isCollecting;
	}

	public int getTransferRateKb() {
		return speed;
	}

	public void abortReadingLog() {
		if(!isCollecting.get())
			return;
		stop();
		props.getLogLoadedProperty().set(false);
		props.getProgressProperty().set(StateProperties.NO_PROGRESS);
		logger.writeLocalMsg("[mgc] Abort reading log");
	}

	@Override
	public void received(Object o) {

		if(!isCollecting.get())
			return;

		if (o instanceof msg_log_entry)
			handleLogEntry((msg_log_entry) o);

		if (o instanceof msg_log_data )
			handleLogData((msg_log_data) o);
	}

	private void handleLogEntry(msg_log_entry entry) {

		last_log_id = entry.num_logs - 1;
		if (last_log_id > -1) {
			if (entry.id != last_log_id)
				requestLogList(last_log_id);
			else {

				if (entry.size == 0) {
					stop();
					return;
				}
				time_utc = entry.time_utc*1000L;
				total_package_count = prepareUnreadPackageList(entry.size);
				System.out.println("Expected packages: " + unread_packages.size()+"/"+entry.size);
				logger.writeLocalMsg("[mgc] Importing Log (" + last_log_id + ") - " + (entry.size / 1024) + " kb");
				try {
					file.setLength(entry.size);
				} catch (IOException e) {
					System.err.println(e.getLocalizedMessage());
					stop();
					return;
				}
				retry = 0;
				state = DATA;
				if(searchForNextUnreadPackage())
				  requestDataPackages(0, chunk_size * LOG_PACKAG_DATA_LENGTH );

			}
		} else {
			stop();
			props.getLogLoadedProperty().set(false);
			props.getProgressProperty().set(StateProperties.NO_PROGRESS);
			wq.removeTask("LP",timeout);
			logger.writeLocalMsg("[mgc] No log available.");
		}
	}

	private void handleLogData(msg_log_data data) {

		retry = 0;

		int p = getPackageNumber(data.ofs);

		if( unread_packages == null || p >= unread_packages.size() || unread_packages.get(p) == -1)
			return;

		try {
			file.seek(data.ofs);
			for (int i = 0; i < data.count; i++)
				file.writeByte((byte) (data.data[i] & 0x000000FF));
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			return;
		}

		//	props.getLogLoadedProperty().set(true);

		unread_packages.set(p, (long) -1);
		read_count++;

		speed = (int)(data.ofs * 1000 / (1024 * (System.currentTimeMillis() - start)));

		fh.setName("in progress ("+speed+"kb/s)");

		int unread_count = unread_packages.size()-read_count; //getUnreadPackageCount();
		//	 System.out.println("Package: "+p +" -> "+unread_packages.get(p)+"-> "+unread_count+" -> "+(unread_packages.size()-read_count));
		props.getProgressProperty().set(1.0f - (float) unread_count / total_package_count);
		if (unread_count == 0) {
			stop();
			sendEndNotice();
			try {
				modelService.getModelList().clear();
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
			DateFormat formatter = new SimpleDateFormat("YYYYMMdd-HHmmss");
			String name = "Log-" + last_log_id + "-" + formatter.format(time_utc);
			copyFileToLogDir(path, name);
			fh.setName(name);
			props.getProgressProperty().set(StateProperties.NO_PROGRESS);
		} 		
	}

	private void stop() {
		wq.removeTask("LP",timeout);
		sendEndNotice();
		state = IDLE;
		isCollecting.set(false);
		try {
			file.close();
		} catch (IOException e) {
		}
	}

	private void copyFileToLogDir(String path, String targetname) {
		Path src  = Paths.get(path);

		String dir = System.getProperty("user.home")+"/Downloads";
		File f = new File(dir);

		if(!f.exists() || !userPrefs.getBoolean(MAVPreferences.DOWNLOAD, true)) {
			return;
		}
		Path dest = Paths.get(dir+"/"+targetname+".ulg");
		try {
			Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			return;
		}
		logger.writeLocalMsg("[mgc] Imported file copied to Downloads");
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

	private int prepareUnreadPackageList(long size) {
		int count = getPackageNumber(size);
		unread_packages = new ArrayList<Long>(count);
		for (long i = 0; i < count + 1; i++)
			unread_packages.add(i * LOG_PACKAG_DATA_LENGTH);
		return count;
	}

	private int getPackageNumber(long offset) {
		return (int) (offset / LOG_PACKAG_DATA_LENGTH);
	}

	private void requestDataPackages(long offset, long len) {
//		 System.out.println("Request packages from: "+offset+ " ("+len+" bytes) "+retry+"re-tries");
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
