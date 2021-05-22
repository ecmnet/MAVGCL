/****************************************************************************
 *
 *   Copyright (c) 2017,2021 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.ui.sidebar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.jfx.extensions.DashLabel;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.Status;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class DetailsWidget extends ChartControlPane {

	private final static int SEPHEIGHT = 12;
	private final static int ROWHEIGHT = 18;

	private final static String STYLE_OUTOFBOUNDS = "-fx-background-color:#2f606e;";
	private final static String STYLE_VALIDDATA = "-fx-background-color:transparent;";
	
	private final static String[] views = { "Flight", "System" };

	private final static String[][] key_figures_details = { 
			
			// Default view
			
		    { 
				"ROLL", "PITCH", "THRUST", null, "GNDV", "CLIMB", "AIRV",
			    null, 
			    "HEAD", "RGPSNO", "RGPSEPH", "RGPSEPV", 
			    null, 
			    "ALTSL", "ALTTR", "ALTGL", "ALTRE",
			    null, 
			    "LIDAR", "FLOWDI", 
			    null, 
			    "LPOSX", "LPOSY", "LPOSZ", 
			    null, 
			    "VISIONX", "VISIONY", "VISIONZ", 
			    null, 
			    "VISIONH", "VISIONR", "VISIONP", 
			    null, 
			    "SLAMDTT", "SLAMDTO", 
			    null,
			    "VISIONFPS", "VISIONQUAL", "FLOWQL","SLAMQU",
			    null, 
			    "BATC", "BATH", "BATP", 
			    null, 
			    "IMUTEMP", "MSPTEMP",  
			    null,  
			    "SWIFI", "RSSI", 
			    null, 
			    "TARM","TBOOT"
			},
		    
		   
		    // System view
		    
		    {
		    	"ROLL", "PITCH", "THRUST", 
		    	null, 
		    	"GNDV", "CLIMB", 
		    	null, 
			    "HEAD", "RGPSNO", "RGPSEPH", "RGPSEPV", 
			    null, 
			    "LIDAR", "FLOWDI", "VISIONZ", "LPOSZ", 
		    	null,
			    "VISIONFPS", "VISIONQUAL", "FLOWQL","SLAMQU",
			    null, 
			    "BATV","BATC", "BATH", "BATP",
			    null, 
			    "IMUTEMP", "MSPTEMP",  "BATT",
			    null,  
			    "CPUPX4", "CPUMSP", "MEMMSP","SWIFI", "RSSI", 
			    null,
			    "DEBUGX","DEBUGY","DEBUGZ",
			    null, 
			    "MAVGCLNET", "MAVGCLACC", 
			    null,
			    "TARM","TBOOT"
		    }
			
	};
	
	@FXML
	private ComboBox<String> view;
	
	@FXML
	private ScrollPane scroll;

	@FXML
	private GridPane grid;

	private Timeline task = null;
	
	private List<KeyFigure> figures = null;

	protected AnalysisDataModel model = AnalysisModelService.getInstance().getCurrent();
	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();
	private Preferences prefs = MAVPreferences.getInstance();
	

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

		task = new Timeline(new KeyFrame(Duration.millis(333), ae -> {
			int i = 0;
			for (KeyFigure figure : figures)
				figure.setValue(model, i++);
			// Workaround to display GPS data whenever GPS is available
			if(control.getCurrentModel().sys.isSensorAvailable(Status.MSP_GPS_AVAILABILITY))
			   state.getGPSAvailableProperty().set(true);
		}));

		task.setCycleCount(Timeline.INDEFINITE);
	}

	@FXML
	private void initialize() {
		
		view.getItems().addAll(views);
		view.setEditable(true);
		view.getEditor().setDisable(true);
		
		view.getSelectionModel().selectedIndexProperty().addListener((v,o,n) -> {
			figures.clear(); grid.getChildren().clear();
			int i = 0;
			for (String k : key_figures_details[n.intValue()]) {
				figures.add(new KeyFigure(grid, k, i));
				i++;
			}
			prefs.putInt(MAVPreferences.VIEW, n.intValue());
//			state.getGPSAvailableProperty().addListener((e, op, np) -> {
//				setBlockVisibility("RGPSNO",np.booleanValue());		  
//			});
		});

		
		this.setWidth(getPrefWidth());
	}

	public void setup(IMAVController control) {
		
		this.control = control;

		scroll.prefHeightProperty().bind(this.heightProperty().subtract(18));
		scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
		scroll.setBorder(Border.EMPTY);
		
		int init_view = prefs.getInt(MAVPreferences.VIEW, 0);
		view.getSelectionModel().clearAndSelect(init_view);
	

		int i = 0;
		for (String k : key_figures_details[init_view]) {
			figures.add(new KeyFigure(grid, k, i));
			i++;
		}
		
		state.getCurrentUpToDate().addListener((e, o, n) -> {
			Platform.runLater(() -> {
				for (KeyFigure figure : figures) {
					if (n.booleanValue())
						figure.setColor(Color.WHITE);
					else
						figure.setColor(Color.LIGHTGRAY);
				}
			});
		});
		
		state.getArmedProperty().addListener((e, o, n) -> {
			if(n.booleanValue())
				view.getSelectionModel().clearAndSelect(0);	
		});

		task.play();

   //     this.disableProperty().bind(state.getLogLoadedProperty().not().and(state.getConnectedProperty().not()));

	}
	


	private class KeyFigure {
		String  key = null;
		int row = 0;
		KeyFigureMetaData kf = null;
		Control value = null;
		GridPane p = null;
		DashLabel label = null;
		Tooltip tip = null;

		double val = 0;

		public KeyFigure(GridPane grid, String k, int r) {
			this.key = k;
			this.row = r;
			p = new GridPane();
			p.setPadding(new Insets(0, 2, 0, 2));
			this.kf = meta.getMetaData(k);
			if (kf == null) {
				value = new Label();
				grid.add(value, 0, row);
				grid.getRowConstraints().add(row, new RowConstraints(SEPHEIGHT, SEPHEIGHT, SEPHEIGHT));
			} else {
				label = new DashLabel(kf.desc1);
				label.setPrefWidth(130);
				label.setPrefHeight(19);
				if (kf.uom.equals("%")) {
					tip = new Tooltip();
					ProgressBar l2 = new ProgressBar();
					l2.setPrefWidth(105);
					value = l2;
					label.setTooltip(tip);
					p.addRow(row, label, l2);
				} else {
					Label l2 = new Label("-");
					l2.setPrefWidth(58);
					l2.setAlignment(Pos.CENTER_RIGHT);
					value = l2;
					Label l3 = new Label(" " + kf.uom);
					l3.setPrefWidth(48);
					p.addRow(row, label, l2, l3);
				}
				grid.add(p, 0, row);
				grid.getRowConstraints().add(row, new RowConstraints(ROWHEIGHT, ROWHEIGHT, ROWHEIGHT));
			}
		}

		public void setColor(Color color) {
			if (label != null)
				label.setTextColor(color);
		}

		public void setValue(AnalysisDataModel model, int row) {

			if (kf != null) {
				val = model.getValue(kf);
				if (Double.isNaN(val)) {
					label.setDashColor(Color.GRAY);
					if (value instanceof ProgressBar)
						((ProgressBar) value).setProgress(0);
					if (value instanceof Label)
						((Label) value).setText("-");
					return;
				}

				if (kf.min != kf.max) {
					if ((val < kf.min || val > kf.max) && state.getConnectedProperty().get()) {
						label.setDashColor(Color.WHITE);
						p.setStyle(STYLE_OUTOFBOUNDS);
					} else {
						label.setDashColor(null);
						p.setStyle(STYLE_VALIDDATA);
					}
				} else {
					label.setDashColor(null);
					p.setStyle(STYLE_VALIDDATA);
				}
				if (value instanceof Label)
					((Label) value).setText(kf.getValueString(val));
				if (value instanceof ProgressBar) {
					tip.setText((int) (val * 100) + "%");
					((ProgressBar) value).setProgress(val);
				}
			}
		}
	}
}
