/****************************************************************************
 *
 *   Copyright (c) 2017,2021 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.video.src.impl.http;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
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
import com.comino.video.src.impl.proxy.MSPVideoProxy;

import javafx.scene.image.Image;

public class MJpegVideoSource  implements IMWVideoSource, Runnable {
	
	private static final boolean PROXY = true;

	private boolean isAvailable;
	private boolean isRunning;
	private int     fps;
	private Thread  thread = null;  

	private MSPVideoMjpegCodec codec;
	private DataInputStream in;
	private Image next;

	private URL     url;
	private long    tms;
	
	private boolean proxy_enabled = PROXY;
	
	private MSPVideoProxy proxy = new MSPVideoProxy();

	private final List<IMWStreamVideoProcessListener> listeners = new ArrayList<IMWStreamVideoProcessListener>();

	public MJpegVideoSource(URI uri, AnalysisDataModel model) {
		try {
			this.url   = uri.toURL();
		} catch (MalformedURLException e) {
		}
		this.codec = new MSPVideoMjpegCodec();
		
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
				LockSupport.parkNanos(40000000);
				processNext();
				if(next!=null) {
					listeners.forEach((listener) -> {
						try {
							listener.process(next, fps, System.currentTimeMillis());
						} catch (Exception e) { e.printStackTrace(); }
					} );
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
		this.listeners.clear();
	}

	@Override
	public void start() {
		if(isRunning)
			return;

		isRunning   = true;
		next = null;

		thread = new Thread(this);
		thread.setName("Video worker");
		thread.start();
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
		conn.setReadTimeout(4000);
		conn.setConnectTimeout(10000);
		conn.setUseCaches(false);
		conn.setRequestProperty("Host", url.getHost());
		conn.setRequestProperty("Client", "chromium");
		conn.connect();
		in = new DataInputStream(conn.getInputStream());
		isAvailable = true;
	}

	private void processNext() throws IOException {
		byte[] data = codec.readFrame(in);
		
		if( data == null) {
			next = null;
		} else {
			if(proxy_enabled)
			  proxy.process(data, data.length);
			next = new Image(new ByteArrayInputStream(data), 0, 0, false, true);
			fps = (int)(((fps * 59) + ((float)(1000f / (System.currentTimeMillis()-tms)))) /60f);
			tms = System.currentTimeMillis();
		}
	}
	

}
