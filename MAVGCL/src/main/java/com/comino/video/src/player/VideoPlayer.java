package com.comino.video.src.player;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.prefs.Preferences;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.control.IMAVController;
import com.comino.video.src.IMWVideoSource;
import com.comino.video.src.impl.http.MJpegVideoSource;
import com.comino.video.src.impl.replay.ReplayMP4VideoSource;
import com.comino.video.src.impl.rtps.RTPSH264VideoSource;
import com.comino.video.src.impl.rtps.RTSPMjpegVideoSource;
import com.comino.video.src.mp4.MP4FFMpegRecorder;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import us.ihmc.log.LogTools;

public class VideoPlayer {

	private static VideoPlayer instance;

	private final ImageView image;
	private final StateProperties state;
	private final Preferences prefs;
	private final IMAVController control;
	private final AnalysisModelService model;

	private IMWVideoSource source;
	private ReplayMP4VideoSource replay_video;
	private MP4FFMpegRecorder recorder = null;

	private boolean isConnected = false;
	private boolean isRecording = false;

	public static VideoPlayer getInstance(IMAVController control, ImageView image, boolean allowRecording) {
		if (instance == null)
			instance = new VideoPlayer(control, image, allowRecording);
		return instance;
	}

	public static VideoPlayer getInstance() {
		return instance;
	}

	private VideoPlayer(IMAVController control, ImageView image, boolean allowRecording) {
		this.image = image;
		this.state = StateProperties.getInstance();
		this.prefs = MAVPreferences.getInstance();
		this.control = control;
		this.model = AnalysisModelService.getInstance();

		this.replay_video = new ReplayMP4VideoSource();

		if (allowRecording)
			recorder = new MP4FFMpegRecorder(prefs.get(MAVPreferences.PREFS_DIR, System.getProperty("user.home")));

		setupStateEvents();
	}

	public void show(boolean show) {

		if (show) {
			if (state.getReplayingProperty().get() || state.getLogLoadedProperty().get()) {
				if (replay_video.open()) {
					replayAtEnd();
				} else {
					image.setVisible(false);
				}
			} else {
				if (replay_video.isOpen() && !state.getLogLoadedProperty().get())
					replay_video.close();
				if (source != null || connect()) {
					if (!source.isRunning()) {
						image.setImage(null);
						source.start();
					}
					image.setVisible(true);
				}
			}
		} else {

			image.setVisible(false);
			if (replay_video.isOpen())
				replay_video.close();
			else {
				if (!state.getMP4RecordingProperty().get() && source != null && source.isRunning()) {
					LogTools.warn("Stopped. Source disconnected " + source.isRunning());
					stop();
				}
			}
		}
	}

	public void changeStreamSource(int stream) {
		if (source == null || !source.isRunning())
			return;
		
		Platform.runLater(() -> {
			LogTools.info("Switching stream");
			source.stop();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
			source.start();
		});
	}

	public void reconnect() {

		if (isConnected || state.getLogLoadedProperty().get()) {
			stop();
			LogTools.warn("Stopped. Try to reconnect");
		}
		image.setImage(null);
		try {
			Thread.sleep(30);
		} catch (InterruptedException e) {
		}
		if (connect()) {
			if (!source.isRunning())
				source.start();
			image.setVisible(true);
		}
	}

	public boolean recording(boolean recording) {

		if (recorder == null) {
			LogTools.warn("No recorder available. Video not recorded");
			return false;
		}

		if (recording == isRecording)
			return false;

		if (recording) {

			if (replay_video.isOpen())
				replay_video.close();

			if (source == null || !isConnected())
				connect();

			long tms = System.currentTimeMillis();
			while (!source.isRunning() && (System.currentTimeMillis() - tms) < 250) {
				source.start();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}

			if (!source.isRunning())
				return false;

			recorder.start();

			isRecording = true;

			return true;

		} else {

			if (state.getMP4RecordingProperty().get()) {

				Platform.runLater(() -> {
					recorder.stop();
					if (!image.isVisible()) {
						stop();
						LogTools.warn("Stopped. Recordingproperty");
					}
				});

				isRecording = false;
				return true;
			}

			return false;
		}

	}

	public void replayAtEnd() {
		final Image img = replay_video.playAt(1.0f);
		Platform.runLater(() -> {
			image.setVisible(true);
			image.setImage(img);

		});
	}

	public void playAtCurrent() {
		if (!replay_video.isOpen())
			return;
		final Image img = replay_video.playAt(model.getCurrent().tms, model.getCurrent().sync_fps);
		Platform.runLater(() -> {
			image.setImage(img);
		});
	}

	public void playAt(float percentage) {
		if (replay_video.isOpen() && image.isVisible()) {
			int x1 = model.calculateIndexByFactor(percentage) - 1;
			if (x1 < 0)
				x1 = 0;
			if (model.getModelList().size() > 0) {
				AnalysisDataModel m = model.getModelList().get(x1);
				final Image img = replay_video.playAt(m.tms, m.sync_fps);
				Platform.runLater(() -> {
					image.setImage(img);
				});
			}

		}
	}

	public void playAtIndex(int index) {
		if (replay_video.isOpen() && image.isVisible()) {
			AnalysisDataModel m = model.getModelList().get(Math.abs(index));
			final Image img = replay_video.playAt(m.tms, m.sync_fps);
			Platform.runLater(() -> {
				image.setImage(img);
			});
		}
	}

	public boolean isConnected() {
		return isConnected;
	}

	private void setupStateEvents() {

		state.getTimeSelectProperty().addListener((e, o, n) -> {
			playAtCurrent();
		});

		state.getCurrentUpToDate().addListener((v, o, n) -> {

			if (state.getReplayingProperty().get())
				return;
			if (!replay_video.isOpen())
				return;
			playAtCurrent();

		});

		state.getLogLoadedProperty().addListener((v, o, n) -> {

			if (state.getRecordingProperty().get() != 0)
				return;

			if (n.booleanValue()) {
				//
				if (replay_video.open()) {
					if (image.isVisible()) {
						image.setVisible(true);
						final Image img = replay_video.playAt(1.0f);
						Platform.runLater(() -> {
							image.setImage(img);

						});
						stop();
						LogTools.warn("Log loaded. Stream stopped");
						return;
					}
				} else {
					stop();
					LogTools.warn("Replay video not opened");
				}

			} else {
				LogTools.info("Replay camera closed. Returning to current streams");
				if (replay_video.isOpen())
					replay_video.close();
				if (state.getConnectedProperty().get()) {
					if (!connect()) {
						image.setVisible(false);
						return;
					}
					source.start();
				} else
					image.setVisible(false);
			}

		});

	}

	public void stop() {
		if (source != null) {
			LogTools.info(source.getClass().getSimpleName() + " stopped");
			source.stop();
			isConnected = false;
		}
	}

	private boolean connect() {
		String url_string = null;

		if (isConnected)
			return true;

		if (prefs.get(MAVPreferences.PREFS_VIDEO, "http://%:1051/stream").contains("%"))
			url_string = prefs.get(MAVPreferences.PREFS_VIDEO, "http://%:1051/stream").replace("%",
					control.getConnectedAddress());
		else
			url_string = prefs.get(MAVPreferences.PREFS_VIDEO, "none");

		try {
			URI url = new URI(url_string);

			LogTools.info("RTSP Connect to " + url.toString());

			if (url.toString().startsWith("http")) {
				source = new MJpegVideoSource(url, model.getCurrent());
			} else if (url.toString().startsWith("rtsp")) {
				source = new RTPSH264VideoSource(url.toString(), control.getCurrentModel());
			} else {
				LogTools.warn("Streaming protocol not supported");
				return false;
			}
			source.removeListeners();

			if (recorder != null)
				source.addProcessListener(recorder);

			source.addProcessListener((im, context) -> {
				if (image.isVisible())
					Platform.runLater(() -> {
						image.setImage(im);
					});
			});
		} catch (URISyntaxException e) {
			LogTools.error(e.getMessage());
			return false;
		}

		isConnected = true;
		return true;
	}

}
