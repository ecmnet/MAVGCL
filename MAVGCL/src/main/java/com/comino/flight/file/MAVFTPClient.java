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
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.LogMessage;
import com.comino.mavcom.model.segment.Status;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class MAVFTPClient {

	private String user = null;
	private String pwd  = null;

	private FtpClient ftp;
	private int       port = 21;

	private FakeFtpServer sitlServer;
	private IMAVController control;

	public MAVFTPClient(IMAVController control) {

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

	public void sendFile(File f) {

		if(f==null || control.getCurrentModel().sys.isStatus(Status.MSP_ARMED)) 
			return;

		ftp = new FtpClient(control.getConnectedAddress(), port, user, pwd);
		try {
			ftp.open();
			ftp.put(f);
			ftp.close();
		} catch (IOException e) { 
			control.writeLogMessage(new LogMessage("[mgc] Selected file "+f.getName()+" could not be sent",MAV_SEVERITY.MAV_SEVERITY_CRITICAL));
			return;
		}
		control.writeLogMessage(new LogMessage("[mgc] File "+f.getName()+" sent to vehicle",MAV_SEVERITY.MAV_SEVERITY_INFO));
	}

	public void close() {
		if(sitlServer!=null)
			sitlServer.stop();
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

	public FakeFtpServer setupSITL() throws IOException {

		FakeFtpServer sitlServer = new FakeFtpServer();
		sitlServer.addUserAccount(new UserAccount("user", "password", "/data"));
		FileSystem fileSystem = new UnixFakeFileSystem();
		fileSystem.add(new DirectoryEntry("/data"));
		sitlServer.setFileSystem(fileSystem);
		sitlServer.setServerControlPort(0);
		sitlServer.start();
		return sitlServer;

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
			ftpc.storeFile(f.getName(), new FileInputStream(f));
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
