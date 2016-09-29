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

package com.comino.flight.widgets.charts.xy;

import com.emxsys.chart.extension.XYAnnotation;

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;

public class StatisticsAnnotation  implements XYAnnotation {

	private final int SIZE = 14;

	private  Pane    pane 		    = null;
	private Ellipse stddev          = null;
	//private Ellipse  minmax         = null;
	private XYStatistics statistics = null;

	public StatisticsAnnotation(Node chart, XYStatistics statistics) {
		this.statistics = statistics;

		this.pane = new Pane();
		this.pane.setPrefSize(SIZE, SIZE);

		this.stddev = new Ellipse();
		this.stddev.setCenterX(SIZE/2);
		this.stddev.setCenterY(SIZE/2);
		this.stddev.setRadiusX(0); this.stddev.setRadiusY(0);
		this.stddev.setStyle("-fx-stroke: rgba(80.0, 80.0, 80.0, 0.75); -fx-fill: rgba(60.0, 60.0, 60.0, 0.35);");
		this.pane.getChildren().addAll(stddev);
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(Math.abs(statistics.stddev_x*xAxis.getScale())>5 && Math.abs(statistics.stddev_y*yAxis.getScale()) > 5) {
		this.stddev.setRadiusX(Math.abs(statistics.stddev_x*xAxis.getScale()));
		this.stddev.setRadiusY(Math.abs(statistics.stddev_y*yAxis.getScale()));
		} else {
			this.stddev.setRadiusX(0); this.stddev.setRadiusY(0);
		}

		pane.setLayoutX(xAxis.getDisplayPosition(statistics.center_x));
		pane.setLayoutY(yAxis.getDisplayPosition(statistics.center_y));
	}

}
