package com.comino.video.src.player;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
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
  private static final String MOVIE_FILE = "/Users/ecmnet/PixHawk/Logs/video.mp4";

  private VideoStream vs;

  @Override public void start(Stage stage) throws Exception {
    vs = new VideoStream(MOVIE_FILE);

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
    final byte[] buf = new byte[15000000];

    timeline.getKeyFrames().setAll(
      new KeyFrame(Duration.ZERO, new EventHandler<ActionEvent>() {
        @Override public void handle(ActionEvent event) {
          try {

            int len = vs.getnextframe(buf);
        	System.out.println("Test "+len);
            if (len == -1) {
              timeline.stop();
              return;
            }
            viewer.setImage(
              new Image(
                new ByteArrayInputStream(
                  Arrays.copyOf(buf, len)
                )
              )
            );
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
          vs = new VideoStream(MOVIE_FILE);
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
      return -1;
    }

    return(fis.read(frame,0,length));
  }
}