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

package com.comino.flight.ui.widgets.gps.details;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.jfx.extensions.DashLabel;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

public class GPSDetailsWidget extends ChartControlPane  {

	private static String[] key_figures = {
			"GLOBLAT",
			"GLOBLON",
			"ALTSL",
			"RGPSLAT",
			"RGPSLON",
			"RGPSNO",
			"BASELAT",
			"BASELON",
			"BASENO",
			"SLAMDIS",
	};

	@FXML
	private GridPane gps_grid;

	private Timeline task = null;
	private AnalysisDataModel model = AnalysisModelService.getInstance().getCurrent();

	private List<KeyFigure> figures = null;

	private DecimalFormat f = new DecimalFormat("0");
	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();


	public GPSDetailsWidget() {
		super();

		figures = new ArrayList<KeyFigure>();

		DecimalFormatSymbols f_symbols = new DecimalFormatSymbols();
		f_symbols.setNaN("-");
		f.setDecimalFormatSymbols(f_symbols);

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("GPSDetailsWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Timeline(new KeyFrame(Duration.millis(1000), ae -> {
			for(KeyFigure figure : figures)
				figure.setValue(model);
		} ) );
		task.setCycleCount(Timeline.INDEFINITE);
	}


	public void setup(IMAVController control) {
		int i=0;
		for(String k : key_figures) {
			figures.add(new KeyFigure(gps_grid,k,i));
			i++;
		}
		task.play();
	}

	private class KeyFigure {
		KeyFigureMetaData kf  = null;
		Label  value = null;

		public KeyFigure(GridPane grid, String k, int row) {
			this.kf = meta.getMetaData(k);
			if(kf==null) {
				grid.add(new Label(),0,row);
			} else {
				DashLabel l1 = new DashLabel(kf.desc1);
				l1.setPrefWidth(140); l1.setPrefHeight(19);
				grid.add(l1, 0, row);
				value = new Label("-"); value.setPrefWidth(100); value.setAlignment(Pos.CENTER_RIGHT);
				grid.add(value, 1, row);
				Label l3 = new Label(" "+kf.uom); l3.setPrefWidth(30);
				grid.add(l3, 2, row);
			}
		}

		public void setValue(AnalysisDataModel model) {
			if(kf!=null) {
				value.setText(kf.getValueString(model.getValue(kf)));
			}
		}
	}
}
