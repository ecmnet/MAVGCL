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

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class DashBoardAnnotation  implements XYAnnotation {

	private  GridPane   pane 		= null;
	private  Label      header     = null;
	private  Label      minmax 	= null;
	private  Label      avg     	= null;
	private int posy;

	public DashBoardAnnotation(int posy) {
        this.posy = posy;
		this.pane = new GridPane();
		pane.setStyle("-fx-background-color: rgba(60.0, 60.0, 60.0, 0.85);");
		header = new Label();
		header.setStyle("-fx-font-size: 8pt;-fx-text-fill: #A0F0A0; -fx-padding:2;");
		minmax = new Label();
		minmax.setStyle("-fx-font-size: 8pt;-fx-text-fill: #B0B0B0; -fx-padding:2;");
		avg = new Label();
		avg.setStyle("-fx-font-size: 8pt;-fx-text-fill: #B0B0B0; -fx-padding:2;");
		this.pane.add(header,0,0);
		this.pane.add(minmax,0,1);
		this.pane.add(avg,0,2);
	}

	public void setHeader(String text) {
		header.setText(text);
	}

	public void setMinMax(float min, float max) {
	  minmax.setText(String.format("Min: % .3f\tMax: % .3f",min,max));
	}

	public void setAvg(float _avg) {
		avg.setText(String.format("Avg: % .3f",_avg));
	}

	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		pane.setLayoutX(10);
		pane.setLayoutY(posy);
	}

}
