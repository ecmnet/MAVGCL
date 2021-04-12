/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.ui.widgets.camera;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.Preferences;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.video.src.IMWVideoSource;
import com.comino.video.src.impl.MJpegVideoSource;
import com.comino.video.src.mp4.MP4Recorder;

import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

public class CameraWidget extends ChartControlPane  {

	private int X = 640 ;
	private int Y = 480 ;


	@FXML
	private ImageView        image;
	private WritableImage     imfx;

	private IMWVideoSource 	source = null;
	private boolean			big_size=false;
	private FloatProperty  	scroll= new SimpleFloatProperty(0);

	private MP4Recorder     recorder = null;
	private boolean         isConnected = false;

	private MSPLogger       logger = null;
	private Preferences userPrefs;



	public CameraWidget() {
		FXMLLoadHelper.load(this, "CameraWidget.fxml");
	}


	@FXML
	private void initialize() {


		fadeProperty().addListener((observable, oldvalue, newvalue) -> {

			if(source==null && !connect()) {
				return;
			}

			if(newvalue.booleanValue())
				source.start();
			else {
				if(!recorder.getRecordMP4Property().get())
					source.stop();
			}
		});

		resize(false,X,Y);

		imfx = new WritableImage(X,Y);


		image.setOnMouseClicked(event -> {

			if(event.getClickCount()==2 && !event.isShiftDown()) {
				if(!big_size)
					big_size=true;
				else
					big_size=false;
				resize(big_size,X,Y);
			} else {
				if(event.isShiftDown()) {
					msg_msp_command msp = new msg_msp_command(255,1);
					msp.command = MSP_CMD.SET_OPTICAL_TARGET;

					msp.param1 =  (float)event.getX();
					msp.param2 =  (float)event.getY();

					control.sendMAVLinkMessage(msp);
				}
			}
			event.consume();

		});


		state.getConnectedProperty().addListener((o,ov,nv) -> {
			image.setImage(null);
			if(fadeProperty().getValue() && !source.isRunning()) {
				if(nv.booleanValue()) {
					connect(); source.start();
				}
			}
		});

		state.getRecordingProperty().addListener((o,ov,nv) -> {
			if(!userPrefs.getBoolean(MAVPreferences.VIDREC, false) || !state.isAutoRecording().get())
				return;

			if(nv.intValue()==AnalysisModelService.COLLECTING) {
				if(source==null)
					connect();
				if(!isConnected)
					return;
				if(!source.isRunning())
					source.start();
				recorder.getRecordMP4Property().set(true);
				logger.writeLocalMsg("[mgc] MP4 recording started", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
			} else {
				if(recorder.getRecordMP4Property().get()) {
					recorder.getRecordMP4Property().set(false);
					logger.writeLocalMsg("[mgc] MP4 recording stopped", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				}
			}

		});

		//		CloseButton close = new CloseButton();
		//		close.setOnMouseClicked(e -> {
		//			this.visibleProperty().set(false);
		//		});
		//
		//		this.getChildren().add(close);

	}

	public FloatProperty getScrollProperty() {
		return scroll;
	}

	private void resize(boolean big, int maxX, int maxY) {
		Platform.runLater(() -> {
			if(big) {
				this.setInitialHeight(maxY); 
				this.setInitialWidth(maxX);
				this.setMaxHeight(maxY);
				this.setMaxWidth(maxX);
				image.setLayoutX(0); image.setFitWidth(maxX);
				image.setLayoutY(0); image.setFitHeight(maxY);
			} else
			{
				this.setInitialHeight(maxY/2); this.setMaxHeight(maxY/2);
				this.setInitialWidth(maxX/2); this.setMaxWidth(maxX/2);
				image.setLayoutX(0); image.setFitWidth(maxX/2);
				image.setLayoutY(0); image.setFitHeight(maxY/2);
			}
		});
	}

	public void setup(IMAVController control) {
		this.control = control;
		userPrefs = MAVPreferences.getInstance();
		logger = MSPLogger.getInstance();
		recorder = new MP4Recorder(userPrefs.get(MAVPreferences.PREFS_DIR, System.getProperty("user.home")),X,Y);
	}


	private boolean connect() {
		String url_string = null;

		if(isConnected)
			return true;

		logger.writeLocalMsg("[mgc] Videosource connected",MAV_SEVERITY.MAV_SEVERITY_DEBUG);

		if(userPrefs.get(MAVPreferences.PREFS_VIDEO,"http://%:8080/mjpeg").contains("%"))
			url_string = userPrefs.get(MAVPreferences.PREFS_VIDEO,"http://%:8080/mjpeg").replace("%", control.getConnectedAddress());
		else
			url_string = userPrefs.get(MAVPreferences.PREFS_VIDEO,"none");


		System.out.println(url_string);

		try {
			URL url = new URL(url_string);
			//		source = new StreamVideoSource(url,AnalysisModelService.getInstance().getCurrent());
			source = new MJpegVideoSource(url,AnalysisModelService.getInstance().getCurrent());
			source.addProcessListener((im, fps) -> {
				if(isVisible())
					Platform.runLater(() -> {
						imfx = SwingFXUtils.toFXImage(im, imfx);
						image.setImage(imfx);

					});
			});
			source.addProcessListener(recorder);
		} catch (MalformedURLException e) {
			return false;
		}
		resize(big_size,X,Y);
		isConnected = true;
		return true;
	}
}
