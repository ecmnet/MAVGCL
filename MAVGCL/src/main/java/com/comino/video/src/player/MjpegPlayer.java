package com.comino.video.src.player;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.api.awt.FrameGrab;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MjpegPlayer extends Application {
	public static void main(String[] args) { Application.launch(MjpegPlayer.class); }

	// ADJUST THIS LOCATION TO SET THE LOCATION OF YOUR MOVIE FILE!!
	private static final String MOVIE_FILE = "/Users/ecmnet/PixHawk/Logs/010521-160341.mp4";
	private long tms_start;


	@Override public void start(Stage stage) throws Exception {


		final ImageView viewer   = new ImageView();
		final Timeline  timeline = createTimeline(viewer);

		VBox layout = new VBox(20);
		layout.setStyle("-fx-background-color: cornsilk;");
		layout.setAlignment(Pos.CENTER);
		layout.getChildren().setAll(
				viewer,
				createControls(timeline)
				);

		stage.setScene(new Scene(layout, 640, 480));
		stage.show();

		timeline.play();
	}

	private Timeline createTimeline(final ImageView viewer) {
		final Timeline timeline = new Timeline();


		timeline.getKeyFrames().setAll(
				new KeyFrame(Duration.ZERO, new EventHandler<ActionEvent>() {
					@Override public void handle(ActionEvent event) {
						try {
							double t = 10000;

							BufferedImage frame = getFrame(new File(MOVIE_FILE), 1);
							if(frame==null) {
								timeline.stop();
								return;
							}



							Image image = SwingFXUtils.toFXImage(frame, null);
							viewer.setImage(image  );
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}),
				new KeyFrame(Duration.seconds(1.0/24))
				);
		timeline.setCycleCount(Timeline.INDEFINITE);

		return timeline;
	}

	private HBox createControls(final Timeline timeline) {
		Button play = new Button("Play");
		play.setOnAction(new EventHandler<ActionEvent>() {


			@Override
			public void handle(ActionEvent event) {
				tms_start = System.currentTimeMillis();
				timeline.play();
			}
		});

		Button pause = new Button("Pause");
		pause.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				timeline.pause();
			}
		});

		Button restart = new Button("Restart");
		restart.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					timeline.stop();
					tms_start = System.currentTimeMillis();
					timeline.playFromStart();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		HBox controls = new HBox(10);
		controls.setAlignment(Pos.CENTER);
		controls.getChildren().setAll(
				play,
				pause,
				restart
				);
		return controls;
	}

	BufferedImage getFrame(File file, double sec) {
		FileChannelWrapper ch = null;
		try {
			ch = NIOUtils.readableFileChannel(file);
			return ((FrameGrab) new FrameGrab(ch).seekToFramePrecise(1)).getFrame();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			NIOUtils.closeQuietly(ch);
		}
		return null;
	}
}