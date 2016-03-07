/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.flight.widgets.details;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.comino.flight.widgets.FadePane;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class DetailsWidget extends FadePane  {


	private static MSTYPE[] key_figures = {
			MSTYPE.MSP_ANGLEX,
			MSTYPE.MSP_ANGLEY,
			MSTYPE.MSP_GRSPEED,
			MSTYPE.MSP_CLIMBRATE,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_COMPASS,
			MSTYPE.MSP_RAW_SATNUM,
			MSTYPE.MSP_GPSEPH,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_ALTAMSL,
			MSTYPE.MSP_ALTLOCAL,
//			MSTYPE.MSP_ALTTERRAIN,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_RAW_FLOWQ,
			MSTYPE.MSP_RAW_DI,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_NEDX,
			MSTYPE.MSP_NEDY,
			MSTYPE.MSP_NEDZ,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_RC0,
			MSTYPE.MSP_RC1,
			MSTYPE.MSP_RC2,
			MSTYPE.MSP_RC3,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_CURRENT,
			MSTYPE.MSP_CONSPOWER,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_VIBX,
			MSTYPE.MSP_VIBY,
			MSTYPE.MSP_VIBZ,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_ABSPRESSURE,
			MSTYPE.MSP_IMUTEMP,


	};

    @FXML
    private GridPane grid;

	private Task<Long> task;
	private DataModel model;

	private List<KeyFigure> figures = null;

	private DecimalFormat f = new DecimalFormat("#0.#");

	public DetailsWidget() {


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
					try {
						Thread.sleep(200);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled() || !isVisible()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}
					updateValue(System.currentTimeMillis());
				}
				return model.battery.tms;
			}
		};

		task.valueProperty().addListener(new ChangeListener<Long>() {

			@Override
			public void changed(ObservableValue<? extends Long> observableValue, Long oldData, Long newData) {
				for(KeyFigure figure : figures) {
					figure.setValue(model);
				}
			}
		});

	}


	public void setup(IMAVController control) {

		int i=0;
		for(MSTYPE k : key_figures) {
			figures.add(new KeyFigure(grid,k,i));
			i++;
		}

		this.model = control.getCurrentModel();
		ExecutorService.get().execute(task);
	}

	private class KeyFigure {
		MSTYPE type  = null;
		Label  value = null;

		public KeyFigure(GridPane grid, MSTYPE k, int row) {
			this.type = k;
			if(k==MSTYPE.MSP_NONE) {
				grid.add(new Label(),0,row);
			} else {
			Label l1 = new Label(k.getDescription()+" :");
			l1.setPrefWidth(85); l1.setPrefHeight(19);
			grid.add(l1, 0, row);
			value = new Label("-"); value.setPrefWidth(45); value.setAlignment(Pos.CENTER_RIGHT);
			grid.add(value, 1, row);
			Label l3 = new Label(" "+k.getUnit()); l3.setPrefWidth(35);
			grid.add(l3, 2, row);
			}
		}

		public void setValue(DataModel model) {
			if(type!=MSTYPE.MSP_NONE)
			  value.setText(f.format(MSTYPE.getValue(model, type)));
		}
	}

}
