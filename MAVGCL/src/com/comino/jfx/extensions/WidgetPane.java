/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.jfx.extensions;

import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;

import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class WidgetPane extends Pane {

	protected StateProperties state = null;

	private FadeTransition in = null;
	private FadeTransition out = null;

	private double mouseX ;
	private double mouseY ;

	private BooleanProperty fade     = new SimpleBooleanProperty(false);
	private BooleanProperty moveable = new SimpleBooleanProperty(false);

	private String prefKey = MAVPreferences.CTRLPOS+this.getClass().getSimpleName().toUpperCase();

	public WidgetPane() {
		this(150);
	}

	public WidgetPane(int duration_ms) {
		this(duration_ms,false);
	}

	public WidgetPane(int duration_ms, boolean visible) {

		this.state = StateProperties.getInstance();
		in = new FadeTransition(Duration.millis(duration_ms), this);
		in.setFromValue(0.0);
		in.setToValue(1.0);

		out = new FadeTransition(Duration.millis(duration_ms), this);
		out.setFromValue(1.0);
		out.setToValue(0.0);

		setVisible(visible);
		fade.set(visible);

		out.setOnFinished(value -> {
			setVisible(false);
		});

		fade.addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(newValue.booleanValue()) {
					if(moveable.get()) {
						setLayoutX(MAVPreferences.getInstance().getDouble(prefKey+"X", 10));
						setLayoutY(MAVPreferences.getInstance().getDouble(prefKey+"Y", 10));
					}
					setVisible(true);
					in.play();
				}
				else
					out.play();
			}
		});

		setOnMousePressed(event -> {
			if(moveable.get()) {
				mouseX = event.getSceneX() ;
				mouseY = event.getSceneY() ;
			}
		});

		setOnMouseDragged(event -> {
			if(moveable.get()) {
				double deltaX = event.getSceneX() - mouseX ;
				double deltaY = event.getSceneY() - mouseY ;
				relocate(getLayoutX() + deltaX, getLayoutY() + deltaY);
				mouseX = event.getSceneX() ;
				mouseY = event.getSceneY() ;
			}
		});

		setOnMouseReleased(event -> {
			if(moveable.get()) {
				MAVPreferences.getInstance().putDouble(prefKey+"X",getLayoutX());
				MAVPreferences.getInstance().putDouble(prefKey+"Y",getLayoutY());
			}
		});

	}

	public BooleanProperty fadeProperty() {
		return fade;
	}

	public BooleanProperty MoveableProperty() {
		return moveable;
	}

	public void setMoveable(boolean val) {
		setManaged(!val); moveable.set(val);
	}

	public boolean getMoveable() {
		return moveable.get();
	}

	public void setInitialWidth(double val) {
		setWidth(val);
	}

	public void setInitialHeight(double val) {
		setHeight(val);
	}

	public double getInitialWidth() {
		return getWidth();
	}

	public double getInitialHeight() {
		return getHeight();
	}

}
