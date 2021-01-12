package com.comino.jfx.extensions;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CloseButton extends Button {

	private final String STYLE_NORMAL = "-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;";

	public CloseButton() {
		setGraphic(new ImageView(new Image(getClass().getResourceAsStream("resources/close_cross.png"))));
		setStyle(STYLE_NORMAL);
	}
}
