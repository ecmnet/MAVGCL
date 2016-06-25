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
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;

public class LineMessageAnnotation  implements XYAnnotation {

	private Circle     circle = null;
	private GridPane   pane   = new GridPane();
	private LogMessage msg    = null;
	private float      xpos   = 0;
	private Label      label  = null;

	public LineMessageAnnotation(float xpos, LogMessage message) {
		this.xpos = xpos;
		this.msg  = message;

		this.circle = new Circle(5);
		switch(message.severity) {
		case MAV_SEVERITY.MAV_SEVERITY_DEBUG:
			this.circle.setFill(Color.CADETBLUE); break;
		case MAV_SEVERITY.MAV_SEVERITY_INFO:
			this.circle.setFill(Color.GREEN); break;
		case MAV_SEVERITY.MAV_SEVERITY_WARNING:
			this.circle.setFill(Color.YELLOW); break;
		case MAV_SEVERITY.MAV_SEVERITY_CRITICAL:
			this.circle.setFill(Color.RED); break;
		default:
			this.circle.setFill(Color.DARKGREY);
		}
		this.circle.setStrokeType(StrokeType.INSIDE);
		this.label = new Label(msg.msg);
		this.label.setVisible(true);
		this.label.setRotate(180);

		label.setStyle("-fx-background-color: rgba(30, 30, 30, 0.2);");

		pane.setHgap(3);
		GridPane.setMargin(circle,new Insets(4,2,4,0));

		pane.addRow(0,circle, label);
		pane.getTransforms().add(new Rotate(90,0,0));

	}

	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		pane.setLayoutX(xAxis.getDisplayPosition(xpos));
		pane.setLayoutY(2);
	}

}
