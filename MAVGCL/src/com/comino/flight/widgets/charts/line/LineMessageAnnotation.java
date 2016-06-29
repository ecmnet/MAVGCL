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

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.msp.model.segment.LogMessage;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;

public class LineMessageAnnotation  implements XYAnnotation {

	private  Polygon triangle = null;
	private float      xpos   = 0;

	public LineMessageAnnotation(float xpos, LogMessage message) {
		this.xpos = xpos;

		this.triangle = new Polygon( 0, 0, 14, 0, 7,10);

		switch(message.severity) {
		case MAV_SEVERITY.MAV_SEVERITY_DEBUG:
			this.triangle.setFill(Color.CADETBLUE); break;
		case MAV_SEVERITY.MAV_SEVERITY_INFO:
			this.triangle.setFill(Color.GREEN); break;
		case MAV_SEVERITY.MAV_SEVERITY_WARNING:
			this.triangle.setFill(Color.YELLOW); break;
		case MAV_SEVERITY.MAV_SEVERITY_CRITICAL:
			this.triangle.setFill(Color.RED); break;
		default:
			this.triangle.setFill(Color.DARKGREY);
		}
		this.triangle.setStrokeType(StrokeType.INSIDE);
		Tooltip t = new Tooltip(message.msg);
		Tooltip.install(triangle, t);
	}

	@Override
	public Node getNode() {
		return triangle;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		triangle.setLayoutX(xAxis.getDisplayPosition(xpos));
		triangle.setLayoutY(0);

	}

}
