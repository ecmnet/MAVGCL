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
package com.comino.flight;

import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class FirstPreloader extends Preloader {

	static final int max = 20;


	Stage stage;
	ProgressBar bar;
	boolean noLoadingProgress = true;

	private Scene createPreloaderScene() {
		VBox splashLayout = new VBox();
		splashLayout.getStylesheets().add(getClass().getResource("splash.css").toExternalForm());
		ImageView splash = new ImageView(new Image(getClass().getResource("splash082.png").toExternalForm()));
		bar = new ProgressBar(0);
		bar.setPrefHeight(5); bar.setPrefWidth(600);
		splashLayout.getChildren().addAll(splash,bar);
		Scene scene =  new Scene(splashLayout, 600,149);
		scene.setFill(Color.rgb(32,32,32));
		return scene;
	}

	public void start(Stage stage) throws Exception {
		this.stage = stage;
		stage.setScene(createPreloaderScene());
		stage.show();
	}

	@Override
	public void handleStateChangeNotification(StateChangeNotification evt) {
		//ignore, hide after application signals it is ready
	}

	@Override
	public void handleApplicationNotification(PreloaderNotification pn) {
		if (pn instanceof ProgressNotification) {
			//           //expect application to send us progress notifications
			//           //with progress ranging from 0 to 1.0
			double v = ((ProgressNotification) pn).getProgress()/max;
			if (!noLoadingProgress) {
				//if we were receiving loading progress notifications
				//then progress is already at 50%.
				//Rescale application progress to start from 50%
				v = 0.5 + v/2;
			}
			final double vf = v;
			Platform.runLater(()->{
				bar.setProgress(vf);
			});
		} else if (pn instanceof StateChangeNotification) {
			//hide after get any state update from application
			bar.setProgress(1);
			stage.hide();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}