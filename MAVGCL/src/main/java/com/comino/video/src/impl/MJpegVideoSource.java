package com.comino.video.src.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.video.src.IMWStreamVideoProcessListener;
import com.comino.video.src.IMWVideoSource;

import boofcv.io.video.VideoMjpegCodec;
import javafx.scene.image.Image;

public class MJpegVideoSource  implements IMWVideoSource, Runnable {

	private boolean isAvailable;
	private boolean isRunning;
	private int     fps;
	private Thread  thread = null;  

	private VideoMjpegCodec codec;
	private DataInputStream in;
	private Image next;

	private URL     url;
	private long    tms;


	private final List<IMWStreamVideoProcessListener> listeners = new ArrayList<IMWStreamVideoProcessListener>();

	public MJpegVideoSource(URL url, AnalysisDataModel model) {
		this.url   = url;
		this.codec = new VideoMjpegCodec();
		
		ImageIO.setUseCache(false);
		Logger.getLogger("javafx.scene.image").setLevel(Level.SEVERE);
	}


	@Override
	public void run() {

		while(isRunning) {

			if(!isAvailable) {
				try {
					connect(url);
				} catch (IOException e) { e.getStackTrace(); }	
				LockSupport.parkNanos(1000000000); continue;
			}

			try {
				LockSupport.parkNanos(10000000);
				processNext();
				if(next!=null) {
					listeners.forEach((listener) -> {
						try {
							listener.process(next, fps);
						} catch (Exception e) { e.printStackTrace(); }
					} );
				} else {
					LockSupport.parkNanos(30000000);
				}
			} catch (IOException e) { e.printStackTrace(); }
		}
		
		System.out.println("Video stopped");
		try {
			in.close();
		} catch (IOException e) {e.printStackTrace(); }
		isAvailable = false;
	}

	@Override
	public void addProcessListener(IMWStreamVideoProcessListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListeners() {

	}

	@Override
	public Thread start() {
		if(isRunning)
			return thread;

		isRunning   = true;

		thread = new Thread(this);
		thread.setName("Video worker");
		thread.start();
		return thread;
	}

	@Override
	public void stop() {
		isRunning = false;

	}

	@Override
	public boolean isAvailable() {
		return isAvailable;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public int getFPS() {
		return fps;
	}

	private void connect(URL url) throws IOException {
		URLConnection conn;
		conn = url.openConnection();
		conn.setReadTimeout(1000);
		conn.setConnectTimeout(10000);
		conn.setUseCaches(false);
		conn.setRequestProperty("Host", url.getHost());
		conn.setRequestProperty("Client", "chromium");
		conn.connect();

		in = new DataInputStream(new BufferedInputStream(conn.getInputStream(),1024*30));
		isAvailable = true;
	}

	private void processNext() throws IOException {
		byte[] data = codec.readFrame(in);
		if( data == null || data.length < 16384) {
			next = null;
		} else {		
			next = new Image(new ByteArrayInputStream(data));
			fps = (int) (fps * 0.7f + (1000 / (System.currentTimeMillis() - tms)) * 0.3f);
			tms = System.currentTimeMillis();
		}
	}
	

}
