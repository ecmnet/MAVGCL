package com.comino.video.src.mp4;

import java.awt.image.BufferedImage;
import java.io.File;

import org.jcodec.api.awt.SequenceEncoder;

import com.comino.video.src.IMWStreamVideoProcessListener;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

//TODO: Framerate to fast

public class MP4Recorder implements IMWStreamVideoProcessListener {

	private MSPSequenceEncoder encoder = null;
	private BooleanProperty recording = new SimpleBooleanProperty();

	private BufferedImage bimg = null;

	public MP4Recorder(int width, int height) {

		recording.addListener((c,o,n) -> {
			try {
				if(n.booleanValue()) {
					System.out.println("MP4 recording started");
					encoder = new MSPSequenceEncoder(new File(System.getProperty("user.home")+"/video.mp4"));
				}
				else {
					System.out.println("MP4 recording stopped");
					encoder.finish();
				}
			}  catch (Exception e1) { e1.printStackTrace(); }
		});
	}

	@Override
	public void process(Image image, byte[] buffer) throws Exception {
		if(recording.get() && image!=null) {
			bimg = SwingFXUtils.fromFXImage(image, bimg);
			encoder.encodeImage(bimg);
		}
	}

	public  BooleanProperty getRecordMP4Property() {
		return recording;
	}

}
