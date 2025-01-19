package com.comino.video.src.impl.rtps;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavutils.rtps.RTPpacket;
import com.comino.video.src.IMWStreamVideoProcessListener;
import com.comino.video.src.IMWVideoSource;
import com.comino.video.src.impl.proxy.MSPVideoProxy;

import javafx.scene.image.Image;
import us.ihmc.log.LogTools;

public class RTSPMjpegVideoSource implements IMWVideoSource {
	
	private static final boolean PROXY = true;


	private DatagramPacket rcvdp;            //UDP packet received from the server
	private DatagramSocket RTPsocket;        //socket to be used to send and receive UDP packets
	private static int RTP_RCV_PORT = 5004; //port where the client will receive the RTP packets

	//Video constants:
	//------------------
	static int MJPEG_TYPE = 26; 			//RTP payload type for MJPEG video

	private boolean isRunning;
	private float   fps;
	private long    tms;
  
	private static BufferedReader RTSPBufferedReader;
	private static BufferedWriter RTSPBufferedWriter;

	private Socket RTSPsocket;           //socket used to send/receive RTSP messages

	private InetAddress ServerIPAddr;
	private int ServerPort = 1051;

	private int RTSPSeqNb = 0;           //Sequence number of RTSP messages within the session
	private String RTSPid;              // ID of the RTSP session (given by the RTSP Server)

	//	private byte[] buf;                 //buffer used to store data received from the server 
	
	private boolean proxy_enabled = PROXY;
//	private MSPVideoProxy proxy = new MSPVideoProxy();


	private final List<IMWStreamVideoProcessListener> listeners = new ArrayList<IMWStreamVideoProcessListener>();


	int statCumLost;            //Number of packets lost
	int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
	int statHighSeqNb;          //Highest sequence number received in session


	private final AnalysisModelService analysis_model;
	private final DataModel            model;

	//private FrameSynchronizer fsynch;

	private final static String CRLF = "\r\n\r\n";

	public RTSPMjpegVideoSource(URI uri, DataModel model) {

		this.analysis_model = AnalysisModelService.getInstance();
		this.model          = model;
		
		//create the frame synchronizer
		//	fsynch = new FrameSynchronizer(100);
		

		try {
			ServerIPAddr = InetAddress.getByName(uri.getHost());
			ServerPort   = uri.getPort();
			System.out.println("VideoSource: "+ServerIPAddr.getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		ImageIO.setUseCache(false);
		Logger.getLogger("javafx.scene.image").setLevel(Level.SEVERE);
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
	public void stop() {

		if(!isRunning)
			return;

		RTSPSeqNb++;
		sendRequest("TEARDOWN");
		isRunning = false;

		//parseServerResponse();
		closeStream();	
		//}
	}

	@Override
	public boolean isAvailable() {
		return isRunning;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public int getFPS() {
		return (int)(fps+0.5f);
	}


	@Override
	public void start() {

		if(isRunning)
			return;
		
		try {

			RTSPsocket = new Socket(ServerIPAddr, ServerPort);
			RTSPsocket.setSoTimeout(1000);

			try {
				//construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
				RTPsocket = new DatagramSocket(RTP_RCV_PORT);
				RTPsocket.setReceiveBufferSize(1024*1024);
			
				RTPsocket.setSoTimeout(60000);
			
			}
			catch (SocketException se) {
				closeStream();
				isRunning = false;
				return;
			}

			//Set input and output stream filters:
			RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
			RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));

		} catch (IOException e) {
			//e.printStackTrace();
			isRunning = false;
			return;
		}

		RTSPSeqNb = 1;
		
//		sendRequest("OPTIONS");
//		//Wait for the response 
//		if (parseServerResponse() != 200) {
//			System.out.println("Invalid Server Response");
//			isRunning = false;
//			return;
//		}
//		
//		sendRequest("DESCRIBE");
//		//Wait for the response 
//		if (parseServerResponse() != 200) {
//			System.out.println("Invalid Server Response");
//			isRunning = false;
//			return;
//		}
//		

		sendRequest("SETUP");
		//Wait for the response 
		if (parseServerResponse() != 200) {
			System.out.println("Invalid Server Response");
			isRunning = false;
			return;
		}

		isRunning = true;
		new Thread(new Receiver()).start();

		//increase RTSP sequence number
		RTSPSeqNb++;
		//Send PLAY message to the server
		sendRequest("PLAY");
		try {
			//parse status line and extract the reply_code:
			String StatusLine = RTSPBufferedReader.readLine();
			LogTools.info(StatusLine);
			StatusLine = RTSPBufferedReader.readLine();
			LogTools.info(StatusLine);
			StatusLine = RTSPBufferedReader.readLine();
			LogTools.info(StatusLine);
			StatusLine = RTSPBufferedReader.readLine();
			LogTools.info(StatusLine);
			StatusLine = RTSPBufferedReader.readLine();
			LogTools.info(StatusLine);
			StatusLine = RTSPBufferedReader.readLine();
			LogTools.info(StatusLine);
			StatusLine = RTSPBufferedReader.readLine();
			LogTools.info(StatusLine);
		}	catch(Exception e) { }
		


	}


	private class Receiver implements Runnable {

		private Image next;
		private final byte [] payload = new byte[128*1024];
		private final byte [] buf     = new byte[128*1024]; 
		
		public void run() {
			System.out.println("Video stream started");

			int seqNb = 0; int payload_length; 

			rcvdp = new DatagramPacket(buf, buf.length);
			statExpRtpNb = 0;

			while(isRunning) {
				try {
					//receive the DP from the socket, save time for stats
					RTPsocket.receive(rcvdp);
					
					LogTools.info("received");

					//create an RTPpacket object from the DP
					RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
					seqNb = rtp_packet.getsequencenumber();
					
					//get the payload bitstream from the RTPpacket object
					payload_length = rtp_packet.getpayload_length();
					
					if(payload_length < 1)
						continue;
					
					rtp_packet.getpayload(payload);

					statExpRtpNb++;
					if (seqNb > statHighSeqNb) {
						statHighSeqNb = seqNb;
					}
					if (statExpRtpNb != seqNb) {
						statCumLost++;
					}
					

					//get an Image object from the payload bitstream
					//		fsynch.addFrame(new Image(new ByteArrayInputStream(payload,0,payload_length), 0, 0, false, true), seqNb);
				
					// Calculate the current average FPS and store it in the datamodel for replay.
					if(statExpRtpNb > 0) {
					  fps = ( fps * (statExpRtpNb - 1) + 1000_000f / (rtp_packet.TimeStamp - tms) ) / statExpRtpNb;
					}
					tms = rtp_packet.TimeStamp;
     				analysis_model.getCurrent().sync_fps = fps;
 //    				analysis_model.getCurrent().sync_fps = model.slam.fps;
 //    				System.out.println(fps);
					
//					if(proxy_enabled)
//					  proxy.process(payload, payload_length);
					next = new Image(new BufferedInputStream(new ByteArrayInputStream(payload,0,payload_length)), 0, 0, false, true);
					if(next!=null) {
						listeners.forEach((listener) -> {
							try {
								listener.process(next, fps, rtp_packet.TimeStamp/1000);
							} catch (Exception ex) { ex.printStackTrace(); }
						} );
					} else
						LogTools.info("NO VIDEO");

				}
				catch (InterruptedIOException iioe) { 
					System.err.println(iioe.getLocalizedMessage());
					
				}
				catch (Exception ioe) {	
//					ioe.printStackTrace();
					LogTools.error(ioe.getLocalizedMessage());
					
				}
			}
		}
	}


	private int parseServerResponse() {
		int reply_code = 0;


		try {
			//parse status line and extract the reply_code:
			String StatusLine = RTSPBufferedReader.readLine();
			LogTools.info(StatusLine);
			StringTokenizer tokens = new StringTokenizer(StatusLine);
			tokens.nextToken(); //skip over the RTSP version
			reply_code = Integer.parseInt(tokens.nextToken());
			
			//if reply code is OK get and print the 2 other lines
			if (reply_code == 200) {
				String SeqNumLine = RTSPBufferedReader.readLine();
				RTSPBufferedReader.readLine(); RTSPBufferedReader.readLine();
				String SessionLine = RTSPBufferedReader.readLine();
				tokens = new StringTokenizer(SessionLine);
				String temp = tokens.nextToken();
				if (temp.compareTo("Session:") == 0) {
					RTSPid = tokens.nextToken();
					LogTools.info("Session started: "+RTSPid);;
				}
			} else {
				LogTools.error(StatusLine);;
				RTPsocket.close();
			}
		} catch(SocketTimeoutException t) {
			System.out.println("Timeout");
			RTPsocket.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		return(reply_code);
	}

	private void sendRequest(String request_type) {
		
		try {
			//Use the RTSPBufferedWriter to write to the RTSP socket
			LogTools.info(request_type);;

	


			//check if request_type is equal to "SETUP" and in this case write the 
			//Transport: line advertising to the server the port used to receive 
			//the RTP packets RTP_RCV_PORT
			if (request_type == "SETUP") {
				RTSPBufferedWriter.write(request_type + " rtsp://0.0.0.0:1051/stream RTSP/1.0\nCSeq: " + RTSPSeqNb + "\nTransport: RTP/UDP;multicast;client_port=" + RTP_RCV_PORT + CRLF);
			}
			else if (request_type == "DESCRIBE") {
				RTSPBufferedWriter.write(request_type + " rtsp://0.0.0.0:1051/:1051/stream RTSP/1.0\nCSeq: " + RTSPSeqNb + "\nAccept: application/sdp" + CRLF);
			}
			else {
				//otherwise, write the Session line from the RTSPid field
				RTSPBufferedWriter.write(request_type + " rtsp://0.0.0.0:1051/stream RTSP/1.0\nCSeq: " + RTSPSeqNb + "\nSession: " + RTSPid+ CRLF);
				
			}

			RTSPBufferedWriter.flush();
		} catch(Exception ex) {
			ex.printStackTrace();
		}    
	}

	private void closeStream() {

		System.out.println("Closing video stream");

		isRunning = false;	
		fps = 0;

		RTSPSeqNb = 0;
		statExpRtpNb = 0;          
		statHighSeqNb = 0;
		statCumLost = 0;
		//		fsynch.reset();

		try { RTSPsocket.close(); } catch (IOException e) { }

		RTPsocket.close(); 

		try { 
			RTSPBufferedReader.close();
			RTSPBufferedWriter.close();
		} catch (IOException e) { }
	}

}
