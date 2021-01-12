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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import com.comino.flight.ui.widgets.charts.utils.XYStatistics;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class XYDashBoardAnnotation  implements XYAnnotation {

	private XYStatistics statistics = null;

	private  GridPane   pane 		= null;

	private  Label      header      = new Label();
	private  HLabel      cex 	    = new HLabel("CenterX:");
	private  HLabel      cey 	    = new HLabel("CenterY:");
	private  HLabel      rad        = new HLabel("Radius:");
	private  HLabel      std     	= new HLabel("\u03C3:");
	private  HLabel      dis     	= new HLabel("Distance:");

	private  VLabel      cex_v       = new VLabel();
	private  VLabel      cey_v 	     = new VLabel();
	private  VLabel      rad_v       = new VLabel();
	private  VLabel      std_v     	 = new VLabel();
	private  VLabel      dis_v     	 = new VLabel();

	private int posy;

	private DecimalFormat f = new DecimalFormat("#0.00");

	public XYDashBoardAnnotation(int posy, XYStatistics statistics) {
        this.statistics = statistics;
        this.posy = posy;
		this.pane = new GridPane();
		pane.setStyle("-fx-background-color: rgba(60.0, 60.0, 60.0, 0.85); -fx-padding:2;");
		header.setStyle("-fx-font-size: 8pt;-fx-text-fill: #A0F0A0; -fx-padding:2;");
        this.pane.setHgap(5);
        this.pane.setMinWidth(150);
		this.pane.add(header,0,0);
		GridPane.setColumnSpan(header,4);
		this.pane.addRow(1,cex,cex_v,cey,cey_v);
		this.pane.addRow(2,rad,rad_v,dis,dis_v);
		this.pane.addRow(3,std,std_v);

		DecimalFormatSymbols sym = new DecimalFormatSymbols();
		sym.setNaN("-"); f.setDecimalFormatSymbols(sym);
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		this.dis_v.setValue(statistics.distance);
		this.cex_v.setValue(statistics.center_x);
		this.cey_v.setValue(statistics.center_y);
		this.rad_v.setValue(statistics.radius);
		this.std_v.setValue(statistics.stddev_xy);
		this.header.setText(statistics.getHeader());
		pane.setLayoutX(10);
		pane.setLayoutY(posy);
	}

	private class VLabel extends Label {

		private String old_text;

		public VLabel() {
			super();
			setAlignment(Pos.CENTER_RIGHT);
			setMinWidth(35);
			setStyle("-fx-font-size: 8pt;-fx-text-fill: #D0D0D0; -fx-padding:3;");
		}

		public void setValue(double val) {
			String s = f.format(val);
			if(!s.equals(old_text))  {
			   setText(s);
			   old_text = s;
			}
		}
	}

	private class HLabel extends Label {

		public HLabel(String text) {
			super(text);
			setAlignment(Pos.CENTER_LEFT);
			setMinWidth(30);
			setStyle("-fx-font-size: 8pt;-fx-text-fill: #D0D0D0; -fx-padding:3;");
		}

	}

}
