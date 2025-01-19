package com.comino.video.src.impl.rtps;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;

import com.comino.mavcom.model.DataModel;
import com.comino.video.src.IMWStreamVideoProcessListener;
import com.comino.video.src.IMWVideoSource;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import us.ihmc.log.LogTools;

public class RTPSH264VideoSource implements IMWVideoSource {
	
	private final List<IMWStreamVideoProcessListener> listeners = new ArrayList<IMWStreamVideoProcessListener>();
	
	private boolean isRunning;
	private float   fps;
	private long    tms;
	private long    tms_start;
	private String  url;
	
	
	public RTPSH264VideoSource(String url, DataModel model) {
		this.url = url;
		this.fps = 30;
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
		isRunning = true;
		new Thread(new Receiver()).start();
	}

	@Override
	public void stop() {
		isRunning = false;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public int getFPS() {
		return (int)fps;
	}
	
	private class Receiver implements Runnable {
		
		private Image next;

		@Override
		public void run() {

	        avformat.avformat_network_init();

	        // Open RTSP stream
	        AVFormatContext formatContext = avformat.avformat_alloc_context();
	        AVDictionary options = new AVDictionary(null);
	      //  av_dict_set(options,"timeout",10000,0);
	        if (avformat.avformat_open_input(formatContext, url, null, options) != 0) {
	            LogTools.error("Could not open RTSP stream");
	            return;
	        }

	        // Find stream information
	        if (avformat.avformat_find_stream_info(formatContext, (AVDictionary) null) < 0) {
	        	 LogTools.error("Could not retrieve stream info");
	            return;
	        }

	        // Find the video stream
	        int videoStreamIndex = -1;
	        for (int i = 0; i < formatContext.nb_streams(); i++) {
	            AVStream stream = formatContext.streams(i);
	            if (stream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_VIDEO) {
	                videoStreamIndex = i;
	                break;
	            }
	        }
	        if (videoStreamIndex == -1) {
	        	 LogTools.error("No video stream found");
	            return;
	        }

	        // Set up the codec context
	        AVCodecContext codecContext = avcodec.avcodec_alloc_context3(null);
	        avcodec.avcodec_parameters_to_context(codecContext, formatContext.streams(videoStreamIndex).codecpar());
	        codecContext.codec_id(avcodec.avcodec_find_decoder(codecContext.codec_id()).id());
	        if (avcodec.avcodec_open2(codecContext, avcodec.avcodec_find_decoder(codecContext.codec_id()), (AVDictionary) null) < 0) {
	        	 LogTools.error("Could not open codec");
	            return;
	        }

	        // Set up scaling context
	        SwsContext swsContext = swscale.sws_getContext(
	                codecContext.width(), codecContext.height(), codecContext.pix_fmt(),
	                codecContext.width(), codecContext.height(), avutil.AV_PIX_FMT_BGR24,
	                swscale.SWS_BILINEAR, null, null, (double[]) null);

	        // Allocate frames for decoding and converting
	        AVFrame decodedFrame = avutil.av_frame_alloc();
	        AVFrame rgbFrame = avutil.av_frame_alloc();
	        BytePointer buffer = new BytePointer(avutil.av_malloc(avutil.av_image_get_buffer_size(
	                avutil.AV_PIX_FMT_BGR24, codecContext.width(), codecContext.height(), 1)));
	        avutil.av_image_fill_arrays(rgbFrame.data(), rgbFrame.linesize(), buffer,
	                avutil.AV_PIX_FMT_BGR24, codecContext.width(), codecContext.height(), 1);


	        BufferedImage image = new BufferedImage(
	                codecContext.width(), codecContext.height(), BufferedImage.TYPE_3BYTE_BGR);
	        byte[] imageBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	        
	        AVPacket packet = new AVPacket(); fps = 0;
	        long tms_frame = System.currentTimeMillis();
	        while (isRunning && avformat.av_read_frame(formatContext, packet) >= 0) {
	            if (packet.stream_index() == videoStreamIndex) {
	                if (avcodec.avcodec_send_packet(codecContext, packet) == 0) {
	                    while (avcodec.avcodec_receive_frame(codecContext, decodedFrame) == 0) {
	                        // Convert frame to RGB
	                        swscale.sws_scale(swsContext, decodedFrame.data(), decodedFrame.linesize(),
	                                0, codecContext.height(), rgbFrame.data(), rgbFrame.linesize());
	                        rgbFrame.data(0).get(imageBuffer);
	                        tms = System.currentTimeMillis();
	                        next = SwingFXUtils.toFXImage(image, null);
	                        if(tms > tms_frame)
	                          fps =  1000  / (tms - tms_frame) *0.4f + fps * 0.6f;
	    					if(next!=null && fps < 100) {
	    						listeners.forEach((listener) -> {
	    							try {
	    								listener.process(next, fps, tms-tms_start);
	    							} catch (Exception ex) { ex.printStackTrace(); }
	    						} );
	    					} 
	      
	                    }
	                    LogTools.info(fps);
	                }
	                avcodec.av_packet_unref(packet);
	                tms_frame = System.currentTimeMillis();
	            }
	        }
	        
	        // Cleanup
	        avutil.av_free(buffer);
	        avutil.av_frame_free(decodedFrame);
	        avutil.av_frame_free(rgbFrame);
	        avcodec.avcodec_close(codecContext);
	        avformat.avformat_close_input(formatContext);
	        swscale.sws_freeContext(swsContext);

			
		}
		
	}

}
