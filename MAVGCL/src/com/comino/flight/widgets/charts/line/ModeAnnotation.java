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
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class ModeAnnotation implements XYAnnotation {

	private final Rectangle rectangle = new Rectangle();

	private double min, max;
	private final Orientation orientation;

	public ModeAnnotation(double min, double max, Orientation orientation, Paint fillPaint) {
		this(min, max, orientation, 0, null, fillPaint);
	}

	public ModeAnnotation(double min, double max, Orientation orientation, double strokeWidth, Paint outlinePaint, Paint fillPaint) {

		this.min = min;
		this.max = max;
		this.orientation = orientation;

		rectangle.getStyleClass().add("chart-annotation-field");

		rectangle.setStrokeWidth(strokeWidth);
		rectangle.setStroke(outlinePaint);
		rectangle.setFill(fillPaint);
	}


	@Override
	public Rectangle getNode() {
		return rectangle;
	}


	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		double strokeW = rectangle.getStrokeWidth();

		double x, y, w, h;
		if (orientation == Orientation.HORIZONTAL) {
			x = xAxis.getDisplayPosition(xAxis.getLowerBound()) - strokeW;
			y = yAxis.getDisplayPosition(max);
			w = xAxis.getDisplayPosition(xAxis.getUpperBound()) + strokeW - x;
			h = yAxis.getDisplayPosition(min) - y;
		} else {
			x = xAxis.getDisplayPosition(min);
			y = yAxis.getDisplayPosition(yAxis.getUpperBound()) - strokeW;
			w = xAxis.getDisplayPosition(max) - x;
			h = yAxis.getDisplayPosition(yAxis.getLowerBound()) + strokeW - y;
		}

		rectangle.setX(x);
		rectangle.setY(y);
		rectangle.setWidth(w);
		rectangle.setHeight(h);
	}

	public void setMax(double max) {
		this.max = max;
	}

}

