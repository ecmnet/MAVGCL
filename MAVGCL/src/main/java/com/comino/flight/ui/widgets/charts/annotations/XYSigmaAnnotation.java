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
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class XYSigmaAnnotation  implements XYAnnotation {

	private double    sigma      = 0;
	private  Pane    pane 		= null;
	private double    xpos   	= 0;
	private double    ypos  	    = 0;
	private Circle circle       = null;

	public XYSigmaAnnotation(Color color) {

		this.pane = new Pane();
		this.pane.setPrefSize(0, 0);
		this.pane.setCache(true);

		this.circle = new Circle();
		this.circle.setCenterX(0);
		this.circle.setCenterY(0);
		this.circle.setRadius(0);
		this.circle.setFill(color);


		circle.setStyle("-fx-fill: rgba(20.0, 60.0, 60.0, 0.25);");

		this.pane.getChildren().addAll(circle);
	}

	public void setPosition(double xpos, double ypos, double sigma) {
		this.xpos  = xpos;
		this.ypos  = ypos;
		this.sigma = sigma;
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
		float size = (float)(xAxis.getDisplayPosition(3*sigma) - xAxis.getDisplayPosition(0));
		if(size > 2) {
			pane.setLayoutX(xAxis.getDisplayPosition(xpos)-size/2);
			pane.setLayoutY(yAxis.getDisplayPosition(ypos)-size/2);
			circle.setCenterX(size/2);
			circle.setCenterY(size/2);
			circle.setRadius(size/2);
			circle.setVisible(true);
		} else
			circle.setVisible(false);

	}

}
