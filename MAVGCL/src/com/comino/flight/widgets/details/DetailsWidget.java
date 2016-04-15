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
import java.util.ArrayList;
import java.util.List;

import com.comino.flight.widgets.FadePane;
import com.comino.mav.control.IMAVController;
import com.comino.model.file.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class DetailsWidget extends FadePane  {


	private static MSTYPE[] key_figures = {
			MSTYPE.MSP_ANGLEX,
			MSTYPE.MSP_ANGLEY,
			MSTYPE.MSP_GRSPEED,
			MSTYPE.MSP_CLIMBRATE,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_COMPASS,
			MSTYPE.MSP_RAW_SATNUM,
			MSTYPE.MSP_GPSHDOP,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_ALTAMSL,
			MSTYPE.MSP_ALTLOCAL,
			MSTYPE.MSP_ALTTERRAIN,
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
			MSTYPE.MSP_DEBUGX,
			MSTYPE.MSP_DEBUGY,
			MSTYPE.MSP_DEBUGZ,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_ABSPRESSURE,
			MSTYPE.MSP_NONE,
			MSTYPE.MSP_IMUTEMP,
			MSTYPE.MSP_CPULOAD,


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
					Platform.runLater(() -> {
						for(KeyFigure figure : figures) {
							figure.setValue(model);
						}
					});
				}
				return model.battery.tms;
			}
		};

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
