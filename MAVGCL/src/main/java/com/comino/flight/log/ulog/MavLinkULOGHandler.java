package com.comino.flight.log.ulog;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.mavlink.messages.lquac.msg_log_data;
import org.mavlink.messages.lquac.msg_log_entry;
import org.mavlink.messages.lquac.msg_log_request_data;
import org.mavlink.messages.lquac.msg_log_request_end;
import org.mavlink.messages.lquac.msg_log_request_list;

import com.comino.flight.file.FileHandler;
import com.comino.flight.log.px4log.PX4toModelConverter;
import com.comino.flight.log.ulog.dialog.ULogSelectionDialog;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.mavlink.IMAVLinkListener;
import com.comino.mavcom.param.ParameterAttributes;
import com.comino.mavutils.workqueue.WorkQueue;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import me.drton.jmavlib.log.px4.PX4LogReader;
import me.drton.jmavlib.log.ulog.ULogReader;

public class MavLinkULOGHandler  implements IMAVLinkListener {

	private static MavLinkULOGHandler instance =null;

	public static final int MODE_SELECT = 0;
	public static final int MODE_LAST   = 1;

	private static final int LOG_PACKAG_DATA_LENGTH = 90;

	private int   mode = 0;

	private final IMAVController control;
	private final StateProperties props;
	private final MSPLogger logger;
	private final FileHandler filehandler;
	private final AnalysisModelService modelService;

	private final BooleanProperty is_directory_loaded = new SimpleBooleanProperty();
	private final BooleanProperty is_log_loaded       = new SimpleBooleanProperty();
	private final BooleanProperty is_loading          = new SimpleBooleanProperty();

	private final Map<Integer,msg_log_entry> directory = new HashMap<Integer,msg_log_entry>();

	private int log_count = 0;
	private long start    = 0;
	private int speed     = 0;
	private int retry     = 0;
	private int worker    = 0;
	private int log_id    = 0;

	private int total_package_count = 0;

	private volatile int chunk_offset = 0;
	private volatile int chunk_size = 0;

	private String path = null;
	private RandomAccessFile file = null;

	private int read_count = 0;
	private List<Long> unread_packages = null;

	private long last_package_tms = 0;

	private final WorkQueue wq = WorkQueue.getInstance();

	public static MavLinkULOGHandler getInstance(IMAVController control) {
		if(instance==null)
			instance = new MavLinkULOGHandler(control);
		return instance;
	}

	private MavLinkULOGHandler(IMAVController control) {

		this.control = control;
		this.props = StateProperties.getInstance();
		this.logger = MSPLogger.getInstance();
		this.control.addMAVLinkListener(this);
		this.filehandler = FileHandler.getInstance();
		this.modelService = AnalysisModelService.getInstance();

		is_directory_loaded.addListener((b,o,n) -> {
			if(n.booleanValue()) {
				props.getProgressProperty().set(StateProperties.NO_PROGRESS);
				System.out.println(directory.size()+" log entries found");
				switch(mode) {
				case MODE_SELECT:
					
					if(directory.size() < 1) {
						cancelLoading();
						return;
					}

					filehandler.setName("select log");
					ULogSelectionDialog d = new ULogSelectionDialog(directory);
					int id = d.selectLogId();
					if(id <0) {
						cancelLoading();
						return;
					}
					requestLog(id);

					break;
				case MODE_LAST:
					requestLog(directory.get(directory.size()-1).id);
					break;	
				}	
				props.getLogLoadedProperty().set(false);
			}
		});

		is_log_loaded.addListener((b,o,n) -> {

			System.out.println("Log loaded");
			is_loading.set(false);
			props.getProgressProperty().set(StateProperties.NO_PROGRESS);

			try {
				ULogReader reader = new ULogReader(path);
				UlogtoModelConverter converter = new UlogtoModelConverter(reader, modelService.getModelList());
				converter.doConversion();
				reader.close();
			} catch (Exception e) {
				//				e.printStackTrace();
			}

			logger.writeLocalMsg("[mgc] Import completed (" + speed + " kb/sec)");
			DateFormat formatter = new SimpleDateFormat("YYYYMMdd-HHmmss");
			String name = "Log-" + log_id + "-" + formatter.format(directory.get(log_id).time_utc);
			copyFileToLogDir(path, name);
			filehandler.setName(name);
			props.getLogLoadedProperty().set(true);

		});

	}

	public void getLog(int mode) {

		if(!props.getConnectedProperty().get()) {
			return;
		}

		this.mode = mode;

		try {
			this.path = filehandler.getTempFile().getPath();
			this.file = new RandomAccessFile(path, "rw");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		sendDirectoryRequest();
		filehandler.setName("getting log entries");
	}

	public void cancelLoading() {

		filehandler.setName("Connected");
		props.getLogLoadedProperty().set(false);
		props.getProgressProperty().set(StateProperties.NO_PROGRESS);


		try {
			file.close();
		} catch (IOException e) {
		}

		if(!is_loading.get())
			return;

		log_count = 0;
		is_loading.set(false);

		wq.removeTask("LP", worker);
		sendEndNotice();

	}

	public BooleanProperty isLogLoaded() {
		return is_log_loaded;
	}

	public BooleanProperty isLoading() {
		return is_loading;
	}


	@Override
	public void received(Object o) {

		if (o instanceof msg_log_entry)
			handleLogEntry((msg_log_entry) o);

		if (o instanceof msg_log_data )
			handleLogData((msg_log_data) o);
	}

	private void requestLog(final int id) {

		log_id = id;

		is_log_loaded.set(false);
		is_loading.set(true);

		msg_log_entry entry = directory.get(id);

		total_package_count = prepareUnreadPackageList(entry.size);
		System.out.println("Expected packages: " + unread_packages.size()+"/"+entry.size);
		logger.writeLocalMsg("[mgc] Importing Log (" + id + ") - " + (entry.size / 1024) + " kb");


		start = System.currentTimeMillis();
		retry = 0;
		chunk_offset = 0;
		read_count = 0;


		requestDataPackages(id, 0, total_package_count * LOG_PACKAG_DATA_LENGTH );

		worker = wq.addCyclicTask("LP",15,() -> {

			if((System.currentTimeMillis() - last_package_tms) < 5 )
				return;

			if (++retry > 1000) {
				cancelLoading();
				return;
			}


			filehandler.setName("loading log "+log_id+" ("+speed+"kb/s)");

			int c = 0;
			while(searchForNextUnreadPackage() && c++ < 5) 
				requestDataPackages(id,chunk_offset * LOG_PACKAG_DATA_LENGTH, chunk_size  * LOG_PACKAG_DATA_LENGTH);

		});
	}


	private void handleLogEntry(msg_log_entry entry) {
		
		if(entry.size == 0)
			return;

		directory.put(entry.id,entry);

		if(entry.id == 0) {
			log_count = entry.num_logs;	
		}

		if(log_count > 0)
			props.getProgressProperty().set((float)directory.size() / log_count);

		if(entry.id == entry.last_log_num) {
			Platform.runLater(() -> {
				is_directory_loaded.set(true);
			});
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
			return;
		}

		//	props.getLogLoadedProperty().set(true);

		unread_packages.set(p, (long) -1);
		read_count++;

		speed = (int)(data.ofs * 1000 / (1024 * (System.currentTimeMillis() - start)));

		//	fh.setName("in progress ("+speed+"kb/s)");

		int unread_count = unread_packages.size()-read_count; //getUnreadPackageCount();
		last_package_tms = System.currentTimeMillis();

		// System.out.println("Package: "+p +" -> "+unread_packages.get(p)+"-> "+unread_count+" -> "+(unread_packages.size()-read_count));
		// System.out.println(unread_count +" - " + total_package_count);

		props.getProgressProperty().set(1.0f - (float) unread_count / total_package_count);

		if (unread_count == 0) {
			sendEndNotice();
			wq.removeTask("LP", worker);
			try {
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			is_log_loaded.set(true);
		} 		
	}

	private void requestDataPackages(int id, long offset, long len) {
		//		 System.out.println("Request packages from: "+offset+ " ("+len+" bytes) "+retry+"re-tries");
		msg_log_request_data msg = new msg_log_request_data(255, 1);
		msg.target_component = 1;
		msg.target_system = 1;
		msg.id = id;
		msg.ofs = offset;
		msg.count = len;
		control.sendMAVLinkMessage(msg);
	}

	private void sendDirectoryRequest() {

		directory.clear(); 
		is_directory_loaded.set(false); 
		log_count = 0;

		msg_log_request_list msg = new msg_log_request_list(255, 1);
		msg.target_component = 1;
		msg.target_system = 1;
		msg.start = 0;
		msg.end = 999999;
		control.sendMAVLinkMessage(msg);
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

	private void sendEndNotice() {
		msg_log_request_end msg = new msg_log_request_end(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
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

	private void copyFileToLogDir(String path, String targetname) {
		Path src  = Paths.get(path);

		String dir = System.getProperty("user.home")+"/Downloads";
		File f = new File(dir);

		if(!f.exists() || !MAVPreferences.getInstance().getBoolean(MAVPreferences.DOWNLOAD, true)) {
			return;
		}
		Path dest = Paths.get(dir+"/"+targetname+".ulg");
		try {
			Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		logger.writeLocalMsg("[mgc] Imported file copied to Downloads");
	}
}
