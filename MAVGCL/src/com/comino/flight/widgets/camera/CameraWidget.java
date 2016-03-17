/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.flight.widgets.camera;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.comino.flight.widgets.FadePane;
import com.comino.mav.control.IMAVController;
import com.comino.video.src.IMWVideoSource;
import com.comino.video.src.impl.StreamVideoSource;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.ImageView;

public class CameraWidget extends FadePane  {


	@FXML
	private ImageView image;

	private IMWVideoSource source = null;

	public CameraWidget() {


		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("CameraWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}
	}

	@FXML
	private void initialize() {
		image.setOpacity(0.90);
        fadeProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(newValue.booleanValue())
					source.start();
				else
					source.stop();

			}

        });

	}


	public void setup(IMAVController control, String url_string) {

		try {
			URL url = new URL(url_string);
			source = new StreamVideoSource(url,320, 240);
			source.addProcessListener(im -> {
				image.setImage(im);
			});
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}



}
