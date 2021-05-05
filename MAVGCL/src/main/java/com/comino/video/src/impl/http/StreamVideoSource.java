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

package com.comino.video.src.impl.http;



import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.video.src.IMWStreamVideoProcessListener;
import com.comino.video.src.IMWVideoSource;

import javafx.scene.image.Image;




/**
 * This class is used as a program entry point,
 * an SWT control and thread's runnable.
 */
public class StreamVideoSource  implements IMWVideoSource, Runnable {

	private static final int RATE = 20;

	public static final String BOUNDARY_MARKER_PREFIX  = "--";

	private Thread thread = null;          // thread reading mjpeg stream
	private DataInputStream stream = null; // mjpeg stream
	private URL url = null;


	private boolean m_collecting =false;
	private boolean isRunning = false;
	private boolean isAvailable = true;
	
	private Image image;

	private long tms=0;

	private long trigger = 0;

	private int  fps=0;

	private List<IMWStreamVideoProcessListener> listener = new ArrayList<IMWStreamVideoProcessListener>();


	public StreamVideoSource(URI uri, AnalysisDataModel model) {


		if (url == null)
			throw new NullPointerException();

		
		try {
			this.url = uri.toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		isAvailable = true;
	}

	public void addProcessListener(IMWStreamVideoProcessListener listener) {
		this.listener.add(listener);
	}

	
	public void run()
	{
		StreamSplit ssplit = null;;
		String connectionError = null;
		String ctype = null;
		Hashtable headers = null;
		URLConnection conn = null;

		while(isRunning) {


			if(url==null) {

				continue;
			}

			//
			do {
				try {

					conn = url.openConnection();
					conn.setReadTimeout(5000);
					conn.setConnectTimeout(5000);
					conn.setRequestProperty("Host", url.getHost());
					conn.setRequestProperty("Client", "chromium");
					conn.connect();

					stream = new DataInputStream(new BufferedInputStream(conn.getInputStream(),1024*200));
					ssplit = new StreamSplit(stream);
					headers = ssplit.readHeaders(conn);
					//

					m_collecting = true;

					connectionError = null;
					ctype = (String) headers.get("content-type");
					if (ctype == null) {
						connectionError = "No main content type";
					} else if (ctype.indexOf("text") != -1) {
						String response;

						while ((response = stream.readLine()) != null) {
							System.out.println(response);
						}
						connectionError = "Failed to connect to server (denied?)";
					}

				} catch (Exception e) {
					connectionError = e.getMessage();
					try {
						stream.close();
					} catch (Exception e1) { }
					m_collecting = false;
				//	System.out.println("Connect VS "+connectionError);
				}

			} while(connectionError != null && isRunning && !m_collecting);
			

			isAvailable = true;

			//
			// Boundary will always be something - '--' or '--foobar'
			//
			int bidx = -1;
			if(ctype!=null)
				bidx = ctype.indexOf("boundary=");
			String boundary = StreamSplit.BOUNDARY_MARKER_PREFIX;
			if (bidx != -1) {
				boundary = ctype.substring(bidx + 9);
				ctype = ctype.substring(0, bidx);

				if (boundary.startsWith("\"") && boundary.endsWith("\""))
				{
					boundary = boundary.substring(1, boundary.length()-1);
				}
				if (!boundary.startsWith(StreamSplit.BOUNDARY_MARKER_PREFIX)) {
					boundary = StreamSplit.BOUNDARY_MARKER_PREFIX + boundary;
				}
			}
			//
			// Now if we have a boundary, read up to that.

			//
			try {
				if (ctype.startsWith("multipart/x-mixed-replace"))
					ssplit.skipToBoundary(boundary);
			} catch (Exception e) {  }


			do {

				try {
					if (m_collecting) {
						//
						// Now we have the real type...
						//  More headers (for the part), then the object...
						//
						if (boundary != null) {
							headers = ssplit.readHeaders();
							if (ssplit.isAtStreamEnd()) {
								break;
							}
							ctype = (String) headers.get("content-type");
							if (ctype == null) {
								throw new Exception("No part content type");
							}
						}
						//
						// Mixed Type -> just skip...
						//
						if (ctype.startsWith("multipart/x-mixed-replace")) {
							bidx = ctype.indexOf("boundary=");
							boundary = ctype.substring(bidx + 9);
							ssplit.skipToBoundary(boundary);
						} else {
							//	System.out.println("FC: "+(++framecounter)+"   ");
							byte[] img = ssplit.readToBoundary(boundary);
							if (img.length == 0) {
								System.out.println("BREAK");
								trigger = 0;
								break;
							}
							try {
								if(System.currentTimeMillis() >= trigger) {
									tms = System.currentTimeMillis();
									image = new Image(new ByteArrayInputStream(img));
								//	image = ImageIO.read(new ByteArrayInputStream(img));
									listener.forEach((l) -> {
										try {
										l.process(image, fps, tms);
										} catch (Exception e) {
											e.printStackTrace();
											System.err.println(e.getMessage());
										}
									} );


									fps = (int)(1000 / (System.currentTimeMillis() - tms));
									trigger = System.currentTimeMillis()+RATE;
								}
								else
									Thread.sleep(RATE);

							} catch (Exception e) {
								throw new Exception(e.getMessage());
							}
						}
					}

				} catch (Exception e) {
					m_collecting = false;
					try {
						stream.close();
					} catch (Exception e1) { }
					connectionError = e.getMessage();
				}

			} while (m_collecting);
		}
	}


	public void start() {
		if(isRunning)
			return;

		isRunning = true;
		thread = new Thread(this);
		thread.setName("Video worker");
		thread.start();
		isAvailable = true;
		trigger = System.currentTimeMillis();
		
	}

	/**
	 * Stops video.
	 * @throws IOException
	 */
	public void stop()  {
		if (thread == null)
			return;
		thread.interrupt();
		m_collecting = false;
		isRunning = false;
		try {
			stream.close();
		} catch (Exception e1) { }
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public boolean isRunning() {
		return isRunning;
	}


	public void setTriggerTime(long time) {
		this.trigger = time;
	}


	@Override
	public int getFPS() {
		return fps;
	}



//	private Image getfromjpeg(byte[] in) {
//		return new Image(new ByteArrayInputStream(in));
//	}

	@Override
	public void removeListeners() {
		this.listener.clear();

	}

}


