package com.comino.jfx.extensions;

import javafx.application.Platform;
import javafx.scene.control.Button;

public class StateButton extends Button {

	private boolean state = false;

	public void setState(boolean state) {
		this.state = state;
		Platform.runLater(() -> {
			if(state)
				setStyle("-fx-background-color: #006C6C");
			else
				setStyle("-fx-background-color: #606060");
		});
	}

	public boolean getState() {
		return state;
	}
}
