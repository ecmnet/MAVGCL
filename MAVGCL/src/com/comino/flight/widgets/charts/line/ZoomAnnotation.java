/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.widgets.charts.line;

import com.emxsys.chart.extension.XYAnnotation;

import javafx.geometry.Orientation;
import javafx.scene.chart.ValueAxis;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class ZoomAnnotation implements XYAnnotation {

	private final Rectangle rectangle = new Rectangle();
	double x0,x1;

	public ZoomAnnotation() {


		rectangle.getStyleClass().add("chart-annotation-field");

		rectangle.setStrokeWidth(0);
		rectangle.setFill(Color.AZURE);
	}


	public void setVisible(boolean val) {
		rectangle.setVisible(val);
	}


	@Override
	public Rectangle getNode() {
		return rectangle;
	}


	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		double strokeW = rectangle.getStrokeWidth();

		double	y0 = yAxis.getDisplayPosition(yAxis.getLowerBound()) - strokeW;
		double	yh = yAxis.getDisplayPosition(yAxis.getUpperBound()) + strokeW - y0;


		rectangle.setX(x0);
		rectangle.setY(y0);
		rectangle.setWidth(x1-x0);
		rectangle.setHeight(yh);
	}

	public void setX0(double x0) {
		this.x0 = x0;
	}

	public void setX1(double x1) {
		this.x1 = x1;
	}

}

