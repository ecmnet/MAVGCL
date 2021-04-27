package com.comino.video.src.impl.rtps;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Timer;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.mavutils.rtps.RTCPpacket;
import com.comino.mavutils.rtps.RTPpacket;
import com.comino.video.src.IMWStreamVideoProcessListener;
import com.comino.video.src.IMWVideoSource;

import javafx.scene.image.Image;

public class RTPSMjpegVideoSource implements IMWVideoSource {


	private DatagramSocket RTCPsocket;          //UDP socket for sending RTCP packets
	private static int RTCP_RCV_PORT = 19001;   //port where the client will receive private static int RTCP_PERIOD  = 400;      //How often to send RTCP packets
	private RtcpSender rtcpSender;

	private DatagramPacket rcvdp;            //UDP packet received from the server
	private DatagramSocket RTPsocket;        //socket to be used to send and receive UDP packets
	private static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets

	//Video constants:
	//------------------
	static int MJPEG_TYPE = 26; 			//RTP payload type for MJPEG video

	private boolean isRunning;
	private int     fps;
	private long    tms;

	private static BufferedReader RTSPBufferedReader;
	private static BufferedWriter RTSPBufferedWriter;

	private Socket RTSPsocket;           //socket used to send/receive RTSP messages
	private Timer timer;                 //timer used to receive data from the UDP socket

	private InetAddress ServerIPAddr;
	private int ServerPort = 1051;

	private int RTSPSeqNb = 0;           //Sequence number of RTSP messages within the session
	private String RTSPid;              // ID of the RTSP session (given by the RTSP Server)

	//	private byte[] buf;                 //buffer used to store data received from the server 

	private int state;

	private final List<IMWStreamVideoProcessListener> listeners = new ArrayList<IMWStreamVideoProcessListener>();


	int statCumLost;            //Number of packets lost
	int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
	int statHighSeqNb;          //Highest sequence number received in session

	private FrameSynchronizer fsynch;

	private final static String CRLF = "\r\n";

	public RTPSMjpegVideoSource(URI uri, AnalysisDataModel model) {

		timer = new Timer(20, new ReceiveTimer());
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		//init RTCP packet sender
		rtcpSender = new RtcpSender(400);

		//create the frame synchronizer
		fsynch = new FrameSynchronizer(100);
		
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

	}


	@Override
	public void stop() {

		if(!isRunning)
			return;

		RTSPSeqNb++;
		sendRequest("TEARDOWN");
		rtcpSender.stopSend();
		timer.stop();

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
		return fps;
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
				RTPsocket.setSoTimeout(5);
				
				//UDP socket for sending QoS RTCP packets
				RTCPsocket = new DatagramSocket();
				RTCPsocket.setSoTimeout(1000);
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
			e.printStackTrace();
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

		//increase RTSP sequence number
		RTSPSeqNb++;
		//Send PLAY message to the server
		sendRequest("PLAY");
		timer.start();
		rtcpSender.startSend();
		System.out.println("Video stream started");
		isRunning = true;
	}


	private class ReceiveTimer implements ActionListener {

		private Image next;
		private byte [] payload = new byte[32768];
		private byte [] buf     = new byte[32768];    

		public void actionPerformed(ActionEvent e) {

			//Construct a DatagramPacket to receive data from the UDP socket
			rcvdp = new DatagramPacket(buf, buf.length);

			try {
				//receive the DP from the socket, save time for stats
				RTPsocket.receive(rcvdp);

				//create an RTPpacket object from the DP
				RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
				int seqNb = rtp_packet.getsequencenumber();

				//get the payload bitstream from the RTPpacket object
				int payload_length = rtp_packet.getpayload_length();
				rtp_packet.getpayload(payload);

				statExpRtpNb++;
				if (seqNb > statHighSeqNb) {
					statHighSeqNb = seqNb;
				}
				if (statExpRtpNb != seqNb) {
					statCumLost++;
				}

				//get an Image object from the payload bitstream
				fsynch.addFrame(new Image(new ByteArrayInputStream(payload,0,payload_length), 0, 0, false, true), seqNb);
				fps = (int) (fps * 0.7f + (1000 / (System.currentTimeMillis() - tms)) * 0.3f);
				tms = System.currentTimeMillis();

				//call image receivers
				next = fsynch.nextFrame();
				if(next!=null) {
					listeners.forEach((listener) -> {
						try {
							listener.process(next, fps);
						} catch (Exception ex) { ex.printStackTrace(); }
					} );
				}
			}
			catch (InterruptedIOException iioe) {
			}
			catch (IOException ioe) {	
			}
		}
	}

	private class RtcpSender implements ActionListener {

		private Timer rtcpTimer;

		// Stats variables
		private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
		private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
		private int lastHighSeqNb;      // The last highest Seq number received
		private int lastCumLost;        // The last cumulative packets lost
		private float lastFractionLost; // The last fraction lost

		public RtcpSender(int interval) {
			rtcpTimer = new Timer(interval, this);
			rtcpTimer.setInitialDelay(0);
			rtcpTimer.setCoalesce(true);
		}

		public void actionPerformed(ActionEvent e) {

			// Calculate the stats for this period
			numPktsExpected = statHighSeqNb - lastHighSeqNb;
			numPktsLost = statCumLost - lastCumLost;
			lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
			lastHighSeqNb = statHighSeqNb;
			lastCumLost = statCumLost;

			//To test lost feedback on lost packets
			// lastFractionLost = randomGenerator.nextInt(10)/10.0f;

			RTCPpacket rtcp_packet = new RTCPpacket(lastFractionLost, statCumLost, statHighSeqNb);
			int packet_length = rtcp_packet.getlength();
			byte[] packet_bits = new byte[packet_length];
			rtcp_packet.getpacket(packet_bits);

			try {
				DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, ServerIPAddr, RTCP_RCV_PORT);
				RTCPsocket.send(dp);
			} catch (InterruptedIOException iioe) {

			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		// Start sending RTCP packets
		public void startSend() {
			rtcpTimer.start();
		}

		// Stop sending RTCP packets
		public void stopSend() {
			rtcpTimer.stop();
			lastHighSeqNb = 0;
			lastCumLost = 0;
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
			RTSPBufferedWriter.write(request_type + " " + "movie.mjpeg"+ " RTSP/1.0" + CRLF);

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

		RTSPSeqNb = 0;
		statExpRtpNb = 0;          
		statHighSeqNb = 0;
		statCumLost = 0;
		fsynch.reset();

		try { RTSPsocket.close(); } catch (IOException e) { }

		RTPsocket.close(); 
		RTCPsocket.close();

		try { 
			RTSPBufferedReader.close();
			RTSPBufferedWriter.close();
		} catch (IOException e) { }
	}

}
