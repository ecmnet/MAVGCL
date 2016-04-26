package com.comino.flight.px4log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.mavlink.messages.lquac.msg_log_data;
import org.mavlink.messages.lquac.msg_log_entry;
import org.mavlink.messages.lquac.msg_log_request_data;
import org.mavlink.messages.lquac.msg_log_request_list;

import com.comino.mav.control.IMAVController;
import com.comino.model.file.FileHandler;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.model.DataModel;

import me.drton.jmavlib.log.px4.PX4LogReader;

public class MAVPX4LogReader implements IMAVLinkListener {

	private IMAVController control = null;
	private boolean isCollecting  = false;
	private int     last_log_id   = 0;
	private long  log_bytes_read  = 0;
	private long  log_bytes_total = 0;

	private File tmpfile = null;
	private BufferedOutputStream out = null;


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
		isCollecting = true;
		msg_log_request_list msg = new msg_log_request_list(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
	}

	@Override
	public void received(Object o) {

		if( o instanceof msg_log_entry) {
			msg_log_entry entry = (msg_log_entry) o;
			last_log_id = entry.num_logs - 1;
			if(last_log_id > -1) {
				try {
					out = new BufferedOutputStream(new FileOutputStream(tmpfile));
				} catch (FileNotFoundException e) { e.printStackTrace(); }
				log_bytes_read = 0; log_bytes_total = entry.size;
				System.out.println("Get data from Log "+last_log_id+" Size: "+entry.size);
				msg_log_request_data msg = new msg_log_request_data(255,1);
				msg.target_component = 1;
				msg.target_system = 1;
				msg.id = last_log_id;
				msg.count = entry.size;
				control.sendMAVLinkMessage(msg);
			}
		}

		if( o instanceof msg_log_data) {
			msg_log_data data = (msg_log_data) o;
			System.out.println(getProgress()+"%");
			for(int i=0;i< data.count;i++) {
				try {
					out.write(data.data[i]);
				} catch (IOException e) { e.printStackTrace(); }
			}
			if(data.count < 90) {
				try {
					out.close();
				} catch (IOException e) { e.printStackTrace(); }
				log_bytes_read = data.ofs;
				try {
					ArrayList<DataModel>modelList = new ArrayList<DataModel>();
					PX4LogReader reader = new PX4LogReader(tmpfile.getAbsolutePath());
					PX4toModelConverter converter = new PX4toModelConverter(reader,modelList);
					converter.doConversion();
					control.getCollector().setModelList(modelList);
					MSPLogger.getInstance().writeLocalMsg("Reding log from device finished");
				} catch (Exception e) { e.printStackTrace(); }

				isCollecting = false;
			}
		}
	}

	public int getProgress() {
		return (int)(log_bytes_read * 100 / log_bytes_total);
	}

	public boolean isCollecting() {
		return isCollecting;
	}

}
