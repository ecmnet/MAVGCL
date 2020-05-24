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

import com.comino.flight.model.KeyFigureMetaData;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class DashBoardAnnotation  implements XYAnnotation {

	private  GridPane   pane 		= null;

	private  Label      header      = new Label();
	private  HLabel      min 	    = new HLabel("Min:");
	private  HLabel      max 	    = new HLabel("Max:");
	private  HLabel      delta      = new HLabel("Delta:");
	private  HLabel      avg     	= new HLabel("\u00F8:");
	private  HLabel      std     	= new HLabel("\u03C3:");

	private  VLabel      val_v       = new VLabel();
	private  VLabel      min_v       = new VLabel();
	private  VLabel      max_v 	     = new VLabel();
	private  VLabel      delta_v     = new VLabel();
	private  VLabel      avg_v       = new VLabel();
	private  VLabel      std_v     	 = new VLabel();

	private int posy;

	private KeyFigureMetaData kf;

	public DashBoardAnnotation(int posy) {

		this.posy = posy;
		this.pane = new GridPane();
		pane.setStyle("-fx-background-color: rgba(60.0, 60.0, 60.0, 0.85); -fx-padding:2;");
		header.setStyle("-fx-font-size: 8pt;-fx-text-fill: #32b5db; -fx-padding:2;");
		this.pane.setHgap(5);
		this.pane.setMinWidth(150);
		this.pane.add(header,0,0);
		GridPane.setColumnSpan(header,3);
		this.pane.add(val_v,3,0);
		this.pane.addRow(1,min,min_v,max,max_v);
		this.pane.addRow(2,delta,delta_v);
		this.pane.addRow(3,avg,avg_v,std,std_v);

	}

	public void setPosY(int y) {
		this.posy = y;
	}

	public void setKeyFigure(KeyFigureMetaData kf) {
		this.kf = kf;
		if(kf.uom!=null && kf.uom.length()>0)
			header.setText(kf.desc1+" ["+kf.uom+"]:");
		else
			header.setText(kf.desc1+":");
	}

	public void setMinMax(double min, double max) {
		min_v.setValue(kf.getValueString(min)); max_v.setValue(kf.getValueString(max));
		delta_v.setValue(kf.getValueString(max-min));
	}

	public void setVal(double val, KeyFigureMetaData kfv, boolean show) {
		if(show) {
			val_v.setValue(kfv.getValueString(val));
		}
		val_v.setVisible(show);
	}

	public void setAvg(double avg, double std) {
		avg_v.setValue(kf.getValueString(avg)); std_v.setValue(kf.getValueString(std));
	}

	@Override
	public Node getNode() {
		return pane;
	}

	public void setVisible(boolean v) {
		pane.setVisible(v);
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		pane.setLayoutX(10);
		pane.setLayoutY(posy);
	}

	private class VLabel extends Label {

		private String old_text;

		public VLabel() {
			super();
			setAlignment(Pos.CENTER_RIGHT);
			setMinWidth(35);
			setStyle("-fx-font-size: 8pt;-fx-text-fill: #D0D0D0; -fx-padding:2;");
		}

		public void setValue(String s) {
			if(!s.equals(old_text))  {
				Platform.runLater(() -> setText(s));
				old_text = s;
			}
		}
	}

	private class HLabel extends Label {

		public HLabel(String text) {
			super(text);
			setAlignment(Pos.CENTER_LEFT);
			setMinWidth(30);
			setStyle("-fx-font-size: 8pt;-fx-text-fill: #D0D0D0; -fx-padding:2;");
		}

	}

}
