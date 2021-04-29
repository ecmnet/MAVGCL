package com.comino.video.src.impl.proxy;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.comino.video.src.impl.http.MSPVideoMjpegCodec;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class MSPVideoProxy implements HttpHandler {
	
	private final byte[] header = "--BoundaryString\r\nContent-type:image/jpeg content-length:1\r\n\r\n".getBytes();
	
	private HttpServer server;
	private byte[] buf ;//= new byte[60000];
	private boolean isReady = false;
	private boolean is_running = false;
	private int length;

	public MSPVideoProxy() {
		
		try {
			server = HttpServer.create(new InetSocketAddress(8081),1);
			server.createContext("/mjpeg", this);
			//		server.setExecutor(ExecutorService.get()); // creates a default executor
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}


	public void process(byte[] buf, int length) {
		//System.arraycopy(buf, 0, this.buf, 0, length);
		this.buf = buf;
		this.length = length;
		synchronized(this) {
			isReady = true;
			notify();
		}	
	}

	@Override
	public void handle(HttpExchange he) throws IOException {
		
		is_running = true;
		
		System.out.println("Waiting for videostream");
		
		he.getResponseHeaders().add("content-type","multipart/x-mixed-replace; boundary=--BoundaryString");
		he.sendResponseHeaders(200, 0);

		OutputStream ios = new BufferedOutputStream(he.getResponseBody(),buf.length);
		
		while(is_running) {

			try {

				synchronized(this) {
					if(!isReady) {
						wait(2000);
					}	  
				}
				
				ios.write(header);
				ios.write(buf,0,length);
				ios.write("\r\n\r\n".getBytes());
				
			} catch (InterruptedException i) {  
                  System.out.println(i.getMessage());
			} catch (Exception e) { is_running = false; }
		}

		ios.flush();
		ios.close();
		he.close();
		
	}

}
