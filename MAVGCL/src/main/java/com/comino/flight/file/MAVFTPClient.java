package com.comino.flight.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.prefs.Preferences;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.mavlink.messages.MAV_SEVERITY;

import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.LogMessage;
import com.comino.mavcom.model.segment.Status;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class MAVFTPClient {
	
	private static MAVFTPClient instance = null;

	private String user = null;
	private String pwd  = null;

	private FtpClient ftp;
	private int       port = 21;

	private IMAVController control;
	
	public static MAVFTPClient getInstance(IMAVController control) {
		if(instance == null)
			instance = new MAVFTPClient(control);
		return instance;
	}

	private MAVFTPClient(IMAVController control) {

		this.control = control;

		Preferences prefs = MAVPreferences.getInstance();
		user = prefs.get(MAVPreferences.FTP_USER, null);
		pwd  = prefs.get(MAVPreferences.FTP_PWD, null);
	}

	public void selectAndSendFile(Stage stage) {

		FileChooser fc = getFileDialog("Select file to send to vehicle",System.getProperty("user.home"));
		File f = fc.showOpenDialog(stage);
        sendFile(f);
	}

	public boolean sendFile(File f) {

		if(f==null ) //|| control.getCurrentModel().sys.isStatus(Status.MSP_ARMED)) 
			return false;

		ftp = new FtpClient(control.getConnectedAddress(), port, user, pwd);
		try {
			ftp.open();
			ftp.put(f);
			ftp.close();
		} catch (IOException e) { 
			control.writeLogMessage(new LogMessage("[mgc] "+f.getName()+" not sent",MAV_SEVERITY.MAV_SEVERITY_CRITICAL));
			return false;
		}
		control.writeLogMessage(new LogMessage("[mgc] "+f.getName()+" sent.",MAV_SEVERITY.MAV_SEVERITY_INFO));
		return true;
	}
	
	public boolean sendFileAs(String name, String as) {
		
		File f = new File(name);
		
		if(!f.exists())
			return false;
		
		ftp = new FtpClient(control.getConnectedAddress(), port, user, pwd);
		try {
			ftp.open();
			ftp.put(as,f);
			ftp.close();
		} catch (IOException e) { 
			e.printStackTrace();
			control.writeLogMessage(new LogMessage("[mgc] "+f.getName()+" not sent",MAV_SEVERITY.MAV_SEVERITY_CRITICAL));
			return false;
		}
		control.writeLogMessage(new LogMessage("[mgc] "+f.getName()+" sent.",MAV_SEVERITY.MAV_SEVERITY_INFO));
		return true;
	
	}

	public void close() {
		
	}

	private FileChooser getFileDialog(String title, String initDir, ExtensionFilter...filter) {

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().addAll(filter);
		File f = new File(initDir);

		if(f.exists())
			fileChooser.setInitialDirectory(f);
		else
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

		return fileChooser;
	}

	private class FtpClient {

		private String server;
		private int    port;
		private String user;
		private String password;

		private FTPClient ftpc;



		public FtpClient(String server, int port, String user, String password) {
			super();
			this.server = server;
			this.port = port;
			this.user = user;
			this.password = password;
		}

		public void put(File f) throws FileNotFoundException, IOException {
			put(f.getName(),f);
		}
		
		public void put(String as, File f) throws FileNotFoundException, IOException {
			ftpc.storeFile(as, new FileInputStream(f));
		}

		public void open() throws IOException {
			ftpc = new FTPClient();

			ftpc.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

			ftpc.connect(server, port);
			int reply = ftpc.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftpc.disconnect();
				throw new IOException("Exception in connecting to FTP Server");
			}

			ftpc.login(user, password);
		}

		public void close() throws IOException {
			ftpc.disconnect();
		}
	}

}
