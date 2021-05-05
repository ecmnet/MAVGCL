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
package com.comino.video.src.mp4;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 *
 */
public class MSPSequenceEncoder {
	
	private SeekableByteChannel ch;
	private Picture toEncode;
	private Transform transform;
	private H264Encoder encoder;
	private ArrayList<ByteBuffer> spsList;
	private ArrayList<ByteBuffer> ppsList;
	private FramesMP4MuxerTrack outTrack;
	private ByteBuffer _out;
	private int frameNo;
	private MP4Muxer muxer;
	private int rate;
	
	private byte[] buf = new byte[640*480*4];

	public MSPSequenceEncoder(File out) throws IOException {

		Logger.getLogger("javafx.scene.image").setLevel(Level.SEVERE);

		this.ch = NIOUtils.writableFileChannel(out);

		// Muxer that will store the encoded frames
		muxer = new MP4Muxer(ch, Brand.MP4);

		// Add video track to muxer
		outTrack = muxer.addTrack(TrackType.VIDEO, 15);

		// Allocate a buffer big enough to hold output frames
		_out = ByteBuffer.allocate(640*480 * 25);

		// Create an instance of encoder
		encoder = new H264Encoder();

		// Transform to convert between RGB and YUV
		transform = ColorUtil.getTransform(ColorSpace.RGB, encoder.getSupportedColorSpaces()[0]);

		// Encoder extra data ( SPS, PPS ) to be stored in a special place of
		// MP4
		spsList = new ArrayList<ByteBuffer>();
		ppsList = new ArrayList<ByteBuffer>();

	}

	public void encodeNativeFrame(Picture pic, int fps) throws IOException {
		if (toEncode == null) {
			toEncode = Picture.create(pic.getWidth(), pic.getHeight(), encoder.getSupportedColorSpaces()[0]);
		}

		// Perform conversion
		transform.transform(pic, toEncode);

		// Encode image into H.264 frame, the result is stored in '_out' buffer
		_out.clear();
		ByteBuffer result = encoder.encodeFrame(toEncode, _out);

		// Based on the frame above form correct MP4 packet
		spsList.clear();
		ppsList.clear();
		H264Utils.wipePS(result, spsList, ppsList);
		H264Utils.encodeMOVPacket(result);

		// Add packet to video track
		try {
			outTrack.addFrame(new MP4Packet(result, frameNo, fps, 1, frameNo, true, null, frameNo, 0));
		} catch(IllegalStateException e) {
			return;
		}

		frameNo++;
	}

	public void finish() throws IOException {
		// Push saved SPS/PPS to a special storage in MP4
		outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList, 4));

		// Write MP4 header and finalize recording
		muxer.writeHeader();
		NIOUtils.closeQuietly(ch);
	}

	public void encodeImage(BufferedImage bi, int fps) throws IOException {
		encodeNativeFrame(AWTUtil.fromBufferedImage(bi), fps);
	}

//	public void encodeImage(Image bi, int fps) throws IOException {
//		int w = (int)bi.getWidth();
//		int h = (int)bi.getHeight();
//		Picture dst = Picture.create(w, h, RGB);
//		int[] dstData = dst.getPlaneData(0);
//		
//		bi.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(), buf, 0, w * 4);
//		
//		int off = 0;
//		for (int i = 0; i < h; i++) {
//            for (int j = 0; j < w; j++) {
//            	  dstData[off++] = buf[i*w*4+j*4+2] ;
//            	  dstData[off++] = buf[i*w*4+j*4+1] ;
//            	  dstData[off++] = buf[i*w*4+j*4+0] ;
// //           	  dstData[off++] = buf[i*w*4+j*4+3];
//            	 
//            }
//        }
//		encodeNativeFrame(dst, fps);
//
//	}
}