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
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.mavutils.rtps.RTPpacket;
import com.comino.video.src.IMWStreamVideoProcessListener;
import com.comino.video.src.IMWVideoSource;
import com.comino.video.src.impl.proxy.MSPVideoProxy;

import javafx.scene.image.Image;

public class RTSPMjpegVideoSource implements IMWVideoSource {
	
	private static final boolean PROXY = true;


	private DatagramPacket rcvdp;            //UDP packet received from the server
	private DatagramSocket RTPsocket;        //socket to be used to send and receive UDP packets
	private static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets

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
	private MSPVideoProxy proxy = new MSPVideoProxy();


	private final List<IMWStreamVideoProcessListener> listeners = new ArrayList<IMWStreamVideoProcessListener>();


	int statCumLost;            //Number of packets lost
	int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
	int statHighSeqNb;          //Highest sequence number received in session

	//private FrameSynchronizer fsynch;

	private final static String CRLF = "\r\n";

	public RTSPMjpegVideoSource(URI uri, AnalysisDataModel model) {

		ImageIO.setUseCache(false);
		
		//create the frame synchronizer
		//	fsynch = new FrameSynchronizer(100);

		try {
			ServerIPAddr = InetAddress.getByName(uri.getHost());
			ServerPort   = uri.getPort();
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

		if(parseServerResponse() == 200) {
			closeStream();	
		}
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

			try {
				//construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
				RTPsocket = new DatagramSocket(RTP_RCV_PORT);
				RTPsocket.setReceiveBufferSize(512*1024);
				RTPsocket.setSoTimeout(40);
			
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

	}


	private class Receiver implements Runnable {

		private Image next;
		private final byte [] payload = new byte[30000];
		private final byte [] buf     = new byte[30000];    

		public void run() {
			System.out.println("Video stream started");

			int seqNb = 0; int payload_length;

			rcvdp = new DatagramPacket(buf, buf.length);

			while(isRunning) {
				try {
					//receive the DP from the socket, save time for stats
					RTPsocket.receive(rcvdp);

					//create an RTPpacket object from the DP
					RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
					seqNb = rtp_packet.getsequencenumber();

					//get the payload bitstream from the RTPpacket object
					payload_length = rtp_packet.getpayload_length();
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
					fps = (fps * 0.9f + (1000 / (System.currentTimeMillis() - tms)) * 0.1f);
					tms = System.currentTimeMillis();
				//	System.out.println(fps+ " => "+rcvdp.getLength());
					//call image receivers
					//		next = fsynch.nextFrame();
					if(proxy_enabled)
					  proxy.process(payload, payload_length);
					next = new Image(new BufferedInputStream(new ByteArrayInputStream(payload,0,payload_length)), 0, 0, false, true);
					if(next!=null) {
						listeners.forEach((listener) -> {
							try {
								listener.process(next, (int)(fps+0.5), tms);
							} catch (Exception ex) { ex.printStackTrace(); }
						} );
					}

				}
				catch (InterruptedIOException iioe) { //System.err.println(iioe.getLocalizedMessage());
				}
				catch (Exception ioe) {	
					System.err.println(ioe.getLocalizedMessage());
				}
			}
		}
	}


	private int parseServerResponse() {
		int reply_code = 0;


		try {
			//parse status line and extract the reply_code:
			String StatusLine = RTSPBufferedReader.readLine();
			StringTokenizer tokens = new StringTokenizer(StatusLine);
			tokens.nextToken(); //skip over the RTSP version
			reply_code = Integer.parseInt(tokens.nextToken());

			//if reply code is OK get and print the 2 other lines
			if (reply_code == 200) {

				String SeqNumLine = RTSPBufferedReader.readLine();

				String SessionLine = RTSPBufferedReader.readLine();
				tokens = new StringTokenizer(SessionLine);
				String temp = tokens.nextToken();

				if (temp.compareTo("Session:") == 0) {
					RTSPid = tokens.nextToken();
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		return(reply_code);
	}

	private void sendRequest(String request_type) {
		try {
			//Use the RTSPBufferedWriter to write to the RTSP socket

			//write the request line:
			RTSPBufferedWriter.write(request_type + " RTSP/1.0" + CRLF);

			//write the CSeq line: 
			RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);

			//check if request_type is equal to "SETUP" and in this case write the 
			//Transport: line advertising to the server the port used to receive 
			//the RTP packets RTP_RCV_PORT
			if (request_type == "SETUP") {
				RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
			}
			else if (request_type == "DESCRIBE") {
				RTSPBufferedWriter.write("Accept: application/sdp" + CRLF);
			}
			else {
				//otherwise, write the Session line from the RTSPid field
				RTSPBufferedWriter.write("Session: " + RTSPid + CRLF);
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
