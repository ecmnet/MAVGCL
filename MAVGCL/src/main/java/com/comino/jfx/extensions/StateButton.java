package com.comino.jfx.extensions;

import com.comino.flight.prefs.MAVPreferences;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.util.Duration;

public class StateButton extends Button {

	private boolean is_light = false;
	
	private BooleanProperty selectedProperty = new SimpleBooleanProperty();

	public StateButton() {

		if(MAVPreferences.getInstance().get(MAVPreferences.PREFS_THEME,"").contains("Light")) {
			is_light = true;
		}

		Timeline timeline = new Timeline(new KeyFrame(
				Duration.millis(200),
				ae ->  { setState(selectedProperty.get()); } ));

		this.addEventHandler(ActionEvent.ACTION, event -> {
			timeline.playFromStart();
			Platform.runLater(() -> {
				if(is_light)
					setStyle("-fx-background-color: #A0A0A0");
				else
					setStyle("-fx-background-color: #1a606e");
			});
		});

		this.disabledProperty().addListener((v,o,n) -> {
			if(!selectedProperty.get())
				if(is_light)
					setStyle("-fx-background-color: #E0E0E0");
				else
					setStyle("-fx-background-color: #606060");
		});
	}

	public void setState(boolean state) {
		//		if(state == this.state || isDisabled())
		//			return;
		selectedProperty.set(state);
		Platform.runLater(() -> {
			if(state) {
				if(is_light)
					setStyle("-fx-background-color: #909090; -fx-text-fill: #F0F0F0");
				else
					setStyle("-fx-background-color: #2f606e");
			}
			else {
				if(is_light)
					setStyle("-fx-background-color: #D0D0D0;");
				else
					setStyle("-fx-background-color: #606060");
			}
		});
	}
	
	public BooleanProperty getSelectedProperty() {
		return selectedProperty;
	}

	public boolean getState() {
		return selectedProperty.get();
	}


}
