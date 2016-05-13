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

package com.comino.flight.tabs.experimental;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.comino.flight.experimental.OffboardSimulationUpdater;
import com.comino.flight.experimental.VisionPositionSimulationUpdater;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableColumn.SortType;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class MAVExperimentalTab extends BorderPane  {


	@FXML
	private Button exp1;

	@FXML
	private Button exp2;


	private VisionPositionSimulationUpdater vision = null;
	private OffboardSimulationUpdater offboard = null;


	public MAVExperimentalTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVExperimentalTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}

	@FXML
	private void initialize() {

		exp1.setOnAction((ActionEvent event)-> {
			offboard.start();


		});

		exp2.setOnAction((ActionEvent event)-> {
			offboard.stop();



		});
	}


	public MAVExperimentalTab setup(IMAVController control) {
		vision = new VisionPositionSimulationUpdater(control);
		offboard = new OffboardSimulationUpdater(control);
		return this;
	}


}
