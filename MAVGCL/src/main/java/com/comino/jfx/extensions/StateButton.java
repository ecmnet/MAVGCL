package com.comino.jfx.extensions;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.util.Duration;

public class StateButton extends Button {

	private boolean state 	= false;

	public StateButton() {

		Timeline timeline = new Timeline(new KeyFrame(
				Duration.millis(1500),
				ae ->  { setState(state); } ));

		this.addEventHandler(ActionEvent.ACTION, event -> {
			timeline.playFromStart();
			Platform.runLater(() -> {
				setStyle("-fx-background-color: #1a606e");
			});
		});
		
		this.disabledProperty().addListener((v,o,n) -> {
			setStyle("-fx-background-color: #606060");
		});
	}

	public void setState(boolean state) {
		if(state == this.state || isDisabled())
			return;
		this.state = state;
		Platform.runLater(() -> {
			if(state)
				setStyle("-fx-background-color: #2f606e");
			else
				setStyle("-fx-background-color: #606060");
		});
	}
	
	

	public boolean getState() {
		return state;
	}


}
