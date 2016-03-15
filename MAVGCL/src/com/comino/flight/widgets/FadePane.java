package com.comino.flight.widgets;

import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class FadePane extends Pane {

	private FadeTransition in = null;
	private FadeTransition out = null;


	private BooleanProperty fade = new SimpleBooleanProperty();

	public FadePane() {
		this(150);
	}

	public FadePane(int duration_ms) {

		in = new FadeTransition(Duration.millis(duration_ms), this);
		in.setFromValue(0.0);
		in.setToValue(1.0);

		out = new FadeTransition(Duration.millis(duration_ms), this);
		out.setFromValue(1.0);
		out.setToValue(0.0);

		out.setOnFinished(value -> {
			setVisible(false);
		});

		this.fade.addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(newValue.booleanValue()) {
					setVisible(true);
					in.play();
				}
				else {
					out.play();
				}
			}

		});



	}

	public BooleanProperty fadeProperty() {
		return fade;
	}



}
