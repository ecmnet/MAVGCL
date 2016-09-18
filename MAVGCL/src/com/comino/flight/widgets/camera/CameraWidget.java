/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.widgets.camera;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.Preferences;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.widgets.fx.controls.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.video.src.IMWVideoSource;
import com.comino.video.src.impl.StreamVideoSource;

import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

public class CameraWidget extends WidgetPane  {


	@FXML
	private ImageView image;

	private IMWVideoSource 	source = null;
	private boolean			big_size=false;
	private FloatProperty  	scroll= new SimpleFloatProperty(0);

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
				source.stop();
			}
		});

		resize(false,400,300);

		image.setOnMouseClicked(event -> {

			if(!big_size)
				big_size=true;
			else
				big_size=false;
			resize(big_size,400,300);

		});

	}

	public FloatProperty getScrollProperty() {
		return scroll;
	}

	private void resize(boolean big, int maxX, int maxY) {
		Platform.runLater(() -> {
			if(big) {
				image.setLayoutX(0); image.setFitWidth(maxX);
				image.setLayoutY(0); image.setFitHeight(maxY);
			} else
			{
				image.setLayoutX(maxX/2); image.setFitWidth(maxX/2);
				image.setLayoutY(maxY/2); image.setFitHeight(maxY/2);
			}
		});
	}

	public void setup(IMAVController control) {

	}


	private boolean connect() {
		System.out.println("VideSource connect");
		Preferences userPrefs = MAVPreferences.getInstance();
		String url_string = userPrefs.get(MAVPreferences.PREFS_VIDEO,"none");
		try {
			URL url = new URL(url_string);
			source = new StreamVideoSource(url,AnalysisModelService.getInstance().getCurrent());
			source.addProcessListener((im,buf) -> {
				image.setImage(im);
			});
		} catch (MalformedURLException e) {
			System.out.println("Camera "+e.getMessage());
			return false;
		}
		return true;
	}
}
