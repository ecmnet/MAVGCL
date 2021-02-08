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

import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;



public class DashLabel extends GridPane {

	private Line  line      = null;
	private Label label     = null;

	private String old_value = null;



	public DashLabel() {
		super();
		this.setPadding(new Insets(3,0,3,0));
		this.setHgap(4);

		this.setCache(true);
		this.setCacheHint(CacheHint.SPEED);

		label = new Label(); label.setTextFill(Color.web("#1c6478").brighter());
		line = new Line(); line.setStroke(Color.web("#1c6478").darker());

		this.addColumn(0, label);
		this.addColumn(1, line);
		this.setPrefWidth(999);

		this.label.widthProperty().addListener((v,ov,nv) -> {
			line.setStartX(0.0f);
			line.setStartY(this.prefHeightProperty().floatValue()/2);
			line.setEndX(this.getWidth() - 10 - nv.doubleValue());
			line.setEndY(this.prefHeightProperty().floatValue()/2);
		});
	}

	public DashLabel(String text) {
		this();
		label.setText(text);
	}


	public String getText() {
		return label.getText();
	}

	public void setText(String value) {
		if(value.equals(old_value))
			return;
		label.setText(value);
		old_value = value;
	}

	public void setTextColor(Color color) {
		label.setTextFill(color);
	}

	public void setDashColor(Color color) {
		if(color!=null)
			line.setStroke(color.darker());
		else
			line.setStroke(Color.web("#1c6478").darker());
	}

	public void setTooltip(Tooltip tip) {
		label.setTooltip(tip);
	}

}
