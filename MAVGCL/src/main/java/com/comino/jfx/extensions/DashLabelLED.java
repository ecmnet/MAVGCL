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


import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;


public class DashLabelLED extends GridPane {

	public final static int MODE_OFF = 0;
	public final static int MODE_ON = 1;
	public final static int MODE_BLINK = 2;

	private Line line = null;
	private Label label = null;
	private Circle circle = null;
	private Color color = null;

	private int mode = -1;
	private boolean toggle = false;
	private Timeline timeline;


	public DashLabelLED() {
		super();
		this.color = Color.WHITE;
		this.setPadding(new Insets(3, 0, 3, 0));
		this.setHgap(4);

		label = new Label();
		label.setTextFill(Color.web("#1c6478").brighter());
		line = new Line();
		line.setStroke(Color.web("#1c6478").darker());

		this.addColumn(0, label);
		this.addColumn(1, line);
		this.setPrefWidth(999);

		GridPane.setHgrow(line, Priority.ALWAYS);

		this.circle = new Circle(4);
		this.circle.setFill(Color.TRANSPARENT);
		this.circle.setStrokeType(StrokeType.INSIDE);
		this.circle.setStroke(Color.ANTIQUEWHITE);
		this.addColumn(2, circle);

		this.label.widthProperty().addListener((v,ov,nv) -> {
			line.setStartX(0.0f);
			line.setStartY(this.prefHeightProperty().floatValue()/2);
			line.setEndX(this.getWidth() - 25 - nv.doubleValue());
			line.setEndY(this.prefHeightProperty().floatValue()/2);
		});

		this.disabledProperty().addListener((v,ov,nv) -> {
			if(nv.booleanValue())
			   circle.setFill(Color.TRANSPARENT);
			else {
				switch (mode) {
				case MODE_OFF:
					circle.setFill(Color.TRANSPARENT);
					break;
				case MODE_ON:
					circle.setFill(color);
					break;
				case MODE_BLINK:
					if (timeline != null)
						timeline.play();
					break;
				}
			}
		});
	}

	public DashLabelLED(String text) {
		this();
		label.setText(text);
	}

	public String getText() {
		return label.getText();
	}

	public void setText(String value) {
		label.setText(value);
	}

	public void set(boolean on) {
		if(on) setMode(MODE_ON); else setMode(MODE_OFF);

		if (timeline != null)
		timeline.stop();
	}

	public void setMode(int mode) {

		if (this.mode == mode)
			return;

		if (timeline != null)
			timeline.stop();

		switch (mode) {
		case MODE_OFF:
			circle.setFill(Color.TRANSPARENT);
			break;
		case MODE_ON:
			circle.setFill(color);
			break;
		case MODE_BLINK:
			if (timeline != null)
				timeline.play();
			break;
		}
		this.mode = mode;
	}

	public int getMode() {
		return mode;
	}

	public void setRate(String rate) {
		timeline = new Timeline(new KeyFrame(Duration.millis(Integer.parseInt(rate)), ae -> {
			if (toggle)
				circle.setFill(color);
			else
				circle.setFill(Color.TRANSPARENT);
			toggle = !toggle;
		}));
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.setDelay(Duration.ZERO);
	}

	public String getRate() {
		return null;
	}

	public String getColor() {
		return color.toString();
	}

	public void setColor(String value) {
		this.color = Color.valueOf(value);
	}

	public String toString() {
		return "Mode="+mode;
	}

}
