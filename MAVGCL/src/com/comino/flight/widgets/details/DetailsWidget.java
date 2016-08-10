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

package com.comino.flight.widgets.details;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.widgets.fx.controls.DashLabel;
import com.comino.flight.widgets.fx.controls.WidgetPane;
import com.comino.mav.control.IMAVController;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;

public class DetailsWidget extends WidgetPane  {


	private static String[] key_figures_details = {
			"ROLL",
			"PITCH",
			null,
			"GNDV",
			"CLIMB",
			"AIRV",
			null,
			"HEAD",
			"RGPSNO",
			"RGPSHDOP",
			null,
			"ALTSL",
			"ALTTR",
			"ALTGL",
			null,
			"FLOWQL",
			"LIDAR",
			"FLOWDI",
			null,
			"LPOSX",
			"LPOSY",
			"LPOSZ",
			null,
			"RC0",
			"RC1",
			"RC2",
			"RC3",
			null,
			"BATC",
			"BATH",
			"BATP",
			null,
			"TEMP",
			"CPUPX4",
			"RSSI",
			null,
			"TARM",
			"TBOOT",
	};

	@FXML
	private ScrollPane scroll;

	@FXML
	private GridPane grid;

	private Task<Long> task;

	private List<KeyFigure> figures = null;

	private DecimalFormat f = new DecimalFormat("#0.#");

	protected AnalysisDataModel model = AnalysisModelService.getInstance().getCurrent();

	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();

	public DetailsWidget() {

		DecimalFormatSymbols f_symbols = new DecimalFormatSymbols();
		f_symbols.setNaN("-");
		f.setDecimalFormatSymbols(f_symbols);

		figures = new ArrayList<KeyFigure>();

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DetailsWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				while(true) {
					LockSupport.parkNanos(250000000L);
					if(isDisabled() || !isVisible()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					Platform.runLater(() -> {
						int i=0;
						for(KeyFigure figure : figures) {
							figure.setValue(model,i++);
						}
					});
				}
				return model.tms;
			}
		};

	}


	public void setup(IMAVController control) {

		scroll.prefHeightProperty().bind(this.heightProperty().subtract(18));
		scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
		scroll.setBorder(Border.EMPTY);

		int i=0;
		for(String k : key_figures_details) {
			figures.add(new KeyFigure(grid,k,i));
			i++;
		}

		Thread th = new Thread(task);
		th.setPriority(Thread.MIN_PRIORITY);
		th.setDaemon(true);
		th.start();

		this.disableProperty().bind(StateProperties.getInstance().getLogLoadedProperty());

	}

	private class KeyFigure {
		KeyFigureMetaData kf  = null;
		Control  value = null;
		GridPane p = null;
		DashLabel label = null;

		float val=0, old_val=Float.NaN;

		public KeyFigure(GridPane grid, String k, int row) {
			p = new GridPane();
			p.setPadding(new Insets(0,2,0,2));
			this.kf = meta.getMetaData(k);
			if(kf==null) {
				grid.add(new Label(),0,row);
			} else {
				label = new DashLabel(kf.desc1);
				label.setPrefWidth(130); label.setPrefHeight(19);
				if(kf.uom.contains("%")) {
				ProgressBar l2 = new ProgressBar(); l2.setPrefWidth(105);
				value = l2;
				p.addRow(row, label,l2);
				} else {
				Label l2 = new Label("-"); l2.setPrefWidth(55); l2.setAlignment(Pos.CENTER_RIGHT);
				value = l2;
				Label l3 = new Label(" "+kf.uom); l3.setPrefWidth(50);
				p.addRow(row, label,l2,l3);
				}
				grid.add(p, 0, row);
			}
		}

		public void setValue(AnalysisDataModel model, int row) {
			if(kf!=null) {
				val =model.getValue(kf);
				if(val==old_val)
					return;
				old_val = val;
				if(kf.min!=kf.max) {
					if(val < kf.min || val > kf.max)
						p.setStyle("-fx-background-color:#004040;");
					else
						p.setStyle("-fx-background-color:transparent;");
				}
				f.applyPattern(kf.mask);
				if(value instanceof Label)
				    ((Label)value).setText(f.format(val));
				if(value instanceof ProgressBar)
				    ((ProgressBar)value).setProgress(val);

			}
		}
	}


}
