package com.comino.video.src.player;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.comino.video.src.IMWStreamVideoProcessListener;
import com.comino.video.src.IMWVideoSource;

import javafx.scene.image.Image;

public class MP4Player implements IMWVideoSource {
	
	private Image     next;
	private float      fps;
	private VideoStream vs;
	
	private final List<IMWStreamVideoProcessListener> listeners = new ArrayList<IMWStreamVideoProcessListener>();
	
	private static final String MOVIE_FILE = "/Users/ecmnet/PixHawk/Logs/video.mp4";
	final byte[] buf = new byte[640*480*3];
	
	
	public MP4Player() throws Exception {
		 vs = new VideoStream(MOVIE_FILE);
	}
	
	public void getFrameAt(long tms) throws Exception {
		
		// TODO: Extract the frame at tms
		 int len = vs.getnextframe(buf);

		 next = new Image( new ByteArrayInputStream(buf,0, len));
                
		listeners.forEach((listener) -> {
			try {
				listener.process(next, (int)(fps+0.5), tms);
			} catch (Exception ex) { ex.printStackTrace(); }
		} );
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
	
		
	}

	@Override
	public void stop() {
		
		
	}

	@Override
	public boolean isAvailable() {
		
		return false;
	}

	@Override
	public boolean isRunning() {
		
		return false;
	}

	@Override
	public int getFPS() {
		
		return 0;
	}
	
	class VideoStream {

		  FileInputStream fis; //video file
		  int frame_nb; //current frame nb

		  public VideoStream(String filename) throws Exception{

		    //init variables
		    fis = new FileInputStream(filename);
		    frame_nb = 0;
		  }

		  public int getnextframe(byte[] frame) throws Exception
		  {
		    int length = 0;
		    String length_string;
		    byte[] frame_length = new byte[2];

		    //read current frame length
		    fis.read(frame_length,0,2);

		    ByteBuffer wrapped = ByteBuffer.wrap(frame_length); // big-endian by default
		    length= wrapped.getShort();

		    //transform frame_length to integer
		    length_string = new String(frame_length);

		    try {

		      length = Integer.parseInt(length_string);
		    } catch (Exception e) {
		      e.printStackTrace();
		      System.out.println(length_string);
		    }

		    return(fis.read(frame,0,length));
		  }
	}

}
