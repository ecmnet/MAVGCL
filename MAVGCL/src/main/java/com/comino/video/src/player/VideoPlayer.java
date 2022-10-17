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
import com.comino.video.src.impl.rtps.RTSPMjpegVideoSource;
import com.comino.video.src.mp4.MP4Recorder;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class VideoPlayer {
	
	
	private final ImageView            image;
	private final StateProperties      state;
	private final Preferences          prefs;
	private final IMAVController       control;
	private final AnalysisModelService model ;
	
    private IMWVideoSource    	 source;
    private ReplayMP4VideoSource replay_video;
    private MP4Recorder          recorder = null;
    
    private boolean              isConnected = false;
	

	public VideoPlayer(IMAVController control,ImageView image, boolean allowRecording) {
		this.image   = image;
		this.state   = StateProperties.getInstance();
		this.prefs   = MAVPreferences.getInstance();
		this.control = control;
		this.model   = AnalysisModelService.getInstance();
		
		this.replay_video = new ReplayMP4VideoSource();
		
		if(allowRecording)
		  recorder = new MP4Recorder(prefs.get(MAVPreferences.PREFS_DIR, System.getProperty("user.home")));
		
		setupStateEvents();
	}
	
	
	public void show(boolean show) {
		
		if(show) {
			if(state.getReplayingProperty().get() || state.getLogLoadedProperty().get()) {
				if(replay_video.open()) {
					replayAtEnd();
				} else {
					image.setVisible(false);
				}
			}
			else  {	
				if(replay_video.isOpen() && !state.getLogLoadedProperty().get())
					replay_video.close();
				if(!state.getConnectedProperty().get())
					return;
				if(source!=null || connect()) {
					image.setVisible(true);
					if(!source.isRunning())
						source.start();
				}
			}	
		} else {
			
			image.setVisible(false);
			if(replay_video.isOpen())
				replay_video.close();
			else {
				if(!state.getMP4RecordingProperty().get() && source != null && source.isRunning())
					stopStreaming();
			}	
		}
	}
	
	public boolean recording(boolean recording) {
		
		if(recorder == null)
			return false;
		
		if(recording) {
			
			if(replay_video.isOpen()) 
				replay_video.close();

			if(source==null)
				connect();
			if(!isConnected)
				return false;
			if(!source.isRunning())
				source.start();
			
			recorder.start();
			return true;
		
		} else {
			
			if(state.getMP4RecordingProperty().get()) {
				
				Platform.runLater(() -> {
					recorder.stop();
					if(!image.isVisible())
						stopStreaming();
				});
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
		if(!replay_video.isOpen())
			return;
		final Image img = replay_video.playAt(model.getCurrent().tms,model.getCurrent().sync_fps);
		Platform.runLater(() -> {
			image.setImage(img);
		});
	}
	
	public void playAt(float percentage) {
		if(replay_video.isOpen() && image.isVisible()) {
			int x1 =  model.calculateIndexByFactor(percentage)-1;
			if(x1 < 0) x1 = 0;
			if(model.getModelList().size()>0) {
				AnalysisDataModel m = model.getModelList().get(x1);
				final Image img = replay_video.playAt(m.tms,m.sync_fps);
				Platform.runLater(() -> {
					image.setImage(img);
				});
			}

		}
	}
	

	public void playAtIndex(int index) {
		if(replay_video.isOpen() && image.isVisible()) {
			AnalysisDataModel m = model.getModelList().get(Math.abs(index));
			final Image img = replay_video.playAt(m.tms,m.sync_fps);
			Platform.runLater(() -> {
				image.setImage(img);
			});
		}
	}
	
	public boolean isConnected() {
		return isConnected;
	}
	
	private void setupStateEvents() {
		
		state.getTimeSelectProperty().addListener((e,o,n) -> {
			playAtCurrent();		
		});
		
		state.getCurrentUpToDate().addListener((v,o,n) -> {

			if(state.getReplayingProperty().get())
				return;
			if(!replay_video.isOpen())
				return;
			playAtCurrent();

		});
		
		state.getLogLoadedProperty().addListener((v,o,n) -> {

			if(state.getRecordingProperty().get()!= 0)
				return;

			if(n.booleanValue()) {
				//			
				if(replay_video.open()) {
					if(image.isVisible()) {
						image.setVisible(true);
						final Image img = replay_video.playAt(1.0f);
						Platform.runLater(() -> {
							image.setImage(img);

						});
						stopStreaming();
						return;
					}
				} 
				else {
					stopStreaming();
					System.out.println("Replay video not opened");
				}

			} 
			else {
				System.out.println("Replay camera closed. Returning to current streams");
				if(replay_video.isOpen()) 
					replay_video.close();
				if(state.getConnectedProperty().get()) {
					if(!connect()) {
						image.setVisible(false);
						return;
					}
					source.start();
				}
				else
					image.setVisible(false);
			}

		});
	}
	
	private void stopStreaming() {
		if(isConnected && source != null) {
			System.out.println(source.getClass().getSimpleName()+" stopped");
			source.stop();
		}
	}
	
	private boolean connect() {
		String url_string = null;

		if(isConnected)
			return true;


		if(prefs.get(MAVPreferences.PREFS_VIDEO,"http://%:8080/mjpeg").contains("%"))
			url_string = prefs.get(MAVPreferences.PREFS_VIDEO,"http://%:8080/mjpeg").replace("%", control.getConnectedAddress());
		else
			url_string = prefs.get(MAVPreferences.PREFS_VIDEO,"none");

		try {
			URI url = new URI(url_string);

			System.out.println("try connect to "+url.toString());

			if(url.toString().startsWith("http")) {
				source = new MJpegVideoSource(url,model.getCurrent());
			} 
			else if(url.toString().startsWith("rtsp")) {
				source = new RTSPMjpegVideoSource(url,control.getCurrentModel());
			}
			else {
				System.out.println("Streaming protocol not supported");
				return false;
			}
			source.removeListeners();
			
			if(recorder!=null)
			  source.addProcessListener(recorder);
			
			source.addProcessListener((im, fps, tms) -> {
				if(image.isVisible())
					Platform.runLater(() -> {
						image.setImage(im);

					});
			});
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return false;
		}

		isConnected = true;
		return true;
	}

}
