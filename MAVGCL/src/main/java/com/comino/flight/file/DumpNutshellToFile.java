package com.comino.flight.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_serial_control;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavutils.workqueue.WorkQueue;

public class DumpNutshellToFile {

	private final IMAVController control;

	private final WorkQueue wq = WorkQueue.getInstance();
	private StringBuilder buffer;

	private boolean is_dumping = false;
	private int     timeout    = 0;
	private File    pathToFile;

	public DumpNutshellToFile(IMAVController control) {
		super();
		this.control = control;

		final char bytes[] = new char[127];

		final DoneWorker done = new DoneWorker();

		control.addMAVLinkListener(msg_serial_control.class, (m) -> {
			
			if(!is_dumping)
				return;

			timeout = wq.rescheduleSingleTask("LP", timeout, 200, done);

			final msg_serial_control msg = (msg_serial_control)m;

			int j=0;
			for(int i=0;i<=msg.count && i < msg.data.length;i++) {
				if((msg.data[i] & 0x007F)!=0)
					bytes[j++] = (char)(msg.data[i] & 0x007F);	
			}
			buffer.append(String.copyValueOf(bytes,0,j).replace("[K", ""));	
		});	

	}

	public void dump(String command, String path) {
		buffer = new StringBuilder();
		pathToFile = new File(path+"/"+command.toLowerCase()+".txt");

		try {
			if(!pathToFile.exists())
				pathToFile.createNewFile();
		} catch (IOException e) {
			return;
		}

		is_dumping = true;
		control.sendShellCommand(command);

	}

	private class DoneWorker implements Runnable {
		
		private MSPLogger logger;

		public DoneWorker() {
			this.logger = MSPLogger.getInstance();
		}

		@Override
		public void run() {
			is_dumping = false;
			
			FileWriter writer;
			try {
				writer = new FileWriter(pathToFile);
			    writer.write(buffer.toString());
			    writer.close();
			} catch (IOException e) {
				return;
			}	
			logger.writeLocalMsg("Nutshell DMESG written to file.", MAV_SEVERITY.MAV_SEVERITY_DEBUG);
		}

	}

}
