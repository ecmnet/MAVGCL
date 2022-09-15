package com.comino.video.src.impl.replay;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_close;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_seek_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGR24;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_image_fill_arrays;
import static org.bytedeco.ffmpeg.global.avutil.av_image_get_buffer_size;
import static org.bytedeco.ffmpeg.global.avutil.av_malloc;
import static org.bytedeco.ffmpeg.global.swscale.SWS_BILINEAR;
import static org.bytedeco.ffmpeg.global.swscale.sws_getContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_scale;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FilenameFilter;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.PointerPointer;

import com.comino.flight.file.FileHandler;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;


public class ReplayMP4VideoSource  {

	private final FileHandler fh = FileHandler.getInstance();
	private final AVFormatContext fmt_ctx;
	private final AVPacket pkt;

	private BufferedImage frame;
	private Graphics ctx;
	private Image image;
	private SwsContext sws_ctx;
	private AVCodecContext codec_ctx;


	private AVFrame raw;
	private AVFrame rgb;

	private BytePointer buffer;
	private DataBufferByte frame_buffer;
	private int stream_idx;
	
	private boolean is_opened = false;


	public ReplayMP4VideoSource() {

		pkt     = new AVPacket();
		fmt_ctx = new AVFormatContext(null);

	}
	
	public Image playAt(long time_ms) {

		if(stream_idx < 0)
			return null;
		
		float rate = (float)fmt_ctx.streams(stream_idx).r_frame_rate().num() / 
			         (float)fmt_ctx.streams(stream_idx).r_frame_rate().den();	
		
		return play((long)(time_ms*rate/1000_000f));
	}

	public Image playAt(float percentage) {

		if(stream_idx < 0)
			return null;
		
		float rate = (float)fmt_ctx.streams(stream_idx).avg_frame_rate().num() / 
				     (float)fmt_ctx.streams(stream_idx).avg_frame_rate().den();	
		
		long time=(long)(fmt_ctx.duration()*rate/1000_000f*percentage);
		if(percentage >= 1)
			time = (long)(fmt_ctx.duration()*rate/1000_000)-1;

		return play(time);
	}
	
	
	public void close() {
		
		if(!is_opened)
			return;
		
		is_opened = false;
		stream_idx = -1;

		av_frame_free(raw);
		av_frame_free(rgb);
		avcodec_close(codec_ctx);
		avcodec_free_context(codec_ctx);
		avformat_close_input(fmt_ctx);
		
	}
	
	public boolean isOpen() {
		return is_opened;
	}

	public boolean open() {
		
		stream_idx = -1;

		String vf = getVideoFileName();
		if(vf == null)
			return false;
		
		if(avformat_open_input(fmt_ctx, vf, null, null) < 0)
			return false;
		
		if(avformat_find_stream_info(fmt_ctx, (PointerPointer)null) < 0) {
			return false;
		}

		for (int i = 0; i < fmt_ctx.nb_streams(); i++) {
			if (fmt_ctx.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
				stream_idx = i;
				break;
			}
		}
		if (stream_idx <0)
			return false;

		codec_ctx = avcodec_alloc_context3(null);
		avcodec_parameters_to_context(codec_ctx, fmt_ctx.streams(stream_idx).codecpar());
		AVCodec codec = avcodec_find_decoder(codec_ctx.codec_id());

		if (codec == null) 
			return false;

		if(avcodec_open2(codec_ctx, codec, (PointerPointer)null)<0)
			return false;

		raw = av_frame_alloc();
		rgb = av_frame_alloc();

		int numBytes = av_image_get_buffer_size(AV_PIX_FMT_BGR24, codec_ctx.width(),
				codec_ctx.height(), 1);
		buffer = new BytePointer(av_malloc(numBytes));

		av_image_fill_arrays(rgb.data(), rgb.linesize(),
				buffer, AV_PIX_FMT_BGR24, codec_ctx.width(), codec_ctx.height(), 1);

		sws_ctx = sws_getContext(
				codec_ctx.width(),
				codec_ctx.height(),
				codec_ctx.pix_fmt(),
				codec_ctx.width(),
				codec_ctx.height(),
				AV_PIX_FMT_BGR24,
				SWS_BILINEAR,
				null,
				null,
				(DoublePointer)null
				);

		frame = new BufferedImage(codec_ctx.width(), codec_ctx.height(), BufferedImage.TYPE_3BYTE_BGR);
		ctx   = frame.getGraphics();
		ctx.setFont(new Font("SansSerif", Font.PLAIN, 9));
		frame_buffer = (DataBufferByte)frame.getRaster().getDataBuffer();
		
		is_opened = true;

		return true;
	}
	
	private Image play(long time) {
		if(av_seek_frame(fmt_ctx,0,time,0)<0) 
			return image;

		if(av_read_frame(fmt_ctx, pkt) < 0)
			return image;

		if (pkt.stream_index() == stream_idx) {
			avcodec_send_packet(codec_ctx, pkt);
			avcodec_receive_frame(codec_ctx, raw);

			sws_scale(
					sws_ctx,
					raw.data(),
					raw.linesize(),
					0,
					codec_ctx.height(),
					rgb.data(),
					rgb.linesize()
					);

			buffer.get(frame_buffer.getData());
			ctx.drawString("Replay:",12,45);
			ctx.drawRect(8,35,37,13);
			image = SwingFXUtils.toFXImage(frame, null);
			av_packet_unref(pkt);
			return image;
		}

		return null;
	}


	private String getVideoFileName() {
		
		String dirname  = fh.getCurrentPath();
		String filename = fh.getName();
		
		System.out.println(dirname +" //// "+filename);
		
		if(dirname==null)
			return null;
		File dir = new File(fh.getCurrentPath());
		
		File[] videos = dir.listFiles(new FilenameFilter() {
			public boolean accept(File directory, String fn) {
				if(filename.length() < 1 || !filename.contains("."))
					return false;
				return fn.endsWith(".mp4") ;
			}
		});
		
		if(videos == null || videos.length < 1 || videos[0].length() < 1000) {
			System.out.println("No video found");
			return null;
		}
		else {
			System.out.println(videos[0].getName()+" found in "+dirname);
			return videos[0].getAbsolutePath();
		}
	}

}
