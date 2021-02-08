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

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.mavcom.model.segment.LogMessage;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;

public class LineMessageAnnotation  implements XYAnnotation {

	private  Pane    pane 		= null;
	private  Label   label 		= null;
	private  Polygon triangle 	= null;
	private float    xpos   	= 0;

	public LineMessageAnnotation(Node chart, float xpos, int ypos, LogMessage message, boolean displayLabel) {
		this.xpos = xpos;

		this.pane = new Pane();
		this.pane.setPrefSize(300, 20);
		this.pane.setCache(true);
		this.pane.setCacheHint(CacheHint.SPEED);

		this.pane.setOnScroll(event -> {
			chart.fireEvent(event);
		});

		this.pane.setBackground(null);

		this.triangle = new Polygon( 0, 0, 14, 0, 7,10);
		this.triangle.setCache(true);
		this.triangle.setCacheHint(CacheHint.SPEED);

		switch(message.severity) {
		case MAV_SEVERITY.MAV_SEVERITY_NOTICE:
			triangle.setFill(Color.LIGHTBLUE);
			break;
		case MAV_SEVERITY.MAV_SEVERITY_DEBUG:
			triangle.setFill(Color.LIGHTGREEN);
			break;
		case MAV_SEVERITY.MAV_SEVERITY_WARNING:
			triangle.setFill(Color.WHEAT);
			break;
		case MAV_SEVERITY.MAV_SEVERITY_CRITICAL:
			triangle.setFill(Color.SALMON);
			break;
		case MAV_SEVERITY.MAV_SEVERITY_EMERGENCY:
			triangle.setFill(Color.TOMATO);
			break;
		case MAV_SEVERITY.MAV_SEVERITY_ERROR:
			triangle.setFill(Color.ORANGE);
			break;
		default:
			triangle.setFill(Color.LIGHTGREY);
		}
		this.triangle.setStrokeType(StrokeType.INSIDE);

		Tooltip tooltip = new Tooltip(message.text);
		Tooltip.install(pane, tooltip);


		if(displayLabel) {
			label = new Label(message.text);
			label.setOnScroll(event -> {
				chart.fireEvent(event);
			});
			label.setLayoutY(15+ypos*18);
			label.setLayoutX(-30);
			label.setStyle("-fx-border-color: #707070; -fx-background-color: rgba(40.0, 40.0, 40.0, 0.65); -fx-padding:2;");
			this.pane.getChildren().addAll(triangle, label);
		} else {
			this.pane.getChildren().addAll(triangle);
		}

	}

	@Override
	public Node getNode() {
		return pane;
	}

	
	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		pane.setLayoutX(xAxis.getDisplayPosition(xpos));
		pane.setLayoutY(0);
	}

}
