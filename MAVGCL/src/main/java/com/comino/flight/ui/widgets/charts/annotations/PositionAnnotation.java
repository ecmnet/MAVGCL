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

package com.comino.flight.ui.widgets.charts.annotations;

import com.emxsys.chart.extension.XYAnnotation;

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PositionAnnotation  implements XYAnnotation {

	private static final int SIZE = 14;

	private  Pane    pane 		= null;
	private  Label   label 		= null;
	private double    xpos   		= 0;
	private double    ypos  	    = 0;
	private Circle circle       	= null;

	public PositionAnnotation(double xpos, double ypos, String text, Color color) {
		this.xpos = xpos;
		this.ypos = ypos;

		this.pane = new Pane();
		this.pane.setPrefSize(SIZE, SIZE);
		this.pane.setCache(true);
	//	this.pane.setBackground(null);

		this.circle = new Circle();
		this.circle.setCenterX(SIZE/2);
		this.circle.setCenterY(SIZE/2);
		this.circle.setRadius(SIZE/2);
		this.circle.setFill(color);

		this.label = new Label(text);
		this.label.setLayoutX(4);
		this.label.setLayoutY(1);

		this.pane.getChildren().addAll(circle, label);
	}

	public PositionAnnotation(String text, Color color) {
		this(0,0,text,color.brighter());
	}

	public void setPosition(double xpos, double ypos) {
		this.xpos = xpos;
		this.ypos = ypos;
	}

	public void setVisible(boolean v) {
		this.pane.setVisible(v);
	}

	@Override
	public Node getNode() {
		return pane;
	}

	
	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		pane.setLayoutX(xAxis.getDisplayPosition(xpos)-SIZE/2);
		pane.setLayoutY(yAxis.getDisplayPosition(ypos)-SIZE/2);
	}

}
