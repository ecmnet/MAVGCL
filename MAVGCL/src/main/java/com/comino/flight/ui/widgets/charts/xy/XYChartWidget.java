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

package com.comino.flight.ui.widgets.charts.xy;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import org.mavlink.messages.MSP_AUTOCONTROL_MODE;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.model.service.ICollectorRecordingListener;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.flight.ui.widgets.charts.annotations.PositionAnnotation;
import com.comino.flight.ui.widgets.charts.annotations.XYDashBoardAnnotation;
import com.comino.flight.ui.widgets.charts.annotations.XYEDFAnnotation;
import com.comino.flight.ui.widgets.charts.annotations.XYGridAnnotation;
import com.comino.flight.ui.widgets.charts.annotations.XYSigmaAnnotation;
import com.comino.flight.ui.widgets.charts.annotations.XYSlamAnnotation;
import com.comino.flight.ui.widgets.charts.annotations.XYTrajectoryAnnonation;
import com.comino.flight.ui.widgets.charts.utils.XYCollections;
import com.comino.flight.ui.widgets.charts.utils.XYDataPool;
import com.comino.flight.ui.widgets.charts.utils.XYObservableListWrapper;
import com.comino.flight.ui.widgets.charts.utils.XYStatistics;
import com.comino.jfx.extensions.SectionLineChart;
import com.comino.jfx.extensions.XYAnnotations.Layer;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavutils.MSPMathUtils;
import com.comino.mavutils.workqueue.WorkQueue;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

// TODO: Add planned path

public class XYChartWidget extends BorderPane implements IChartControl, ICollectorRecordingListener {

	private static final int REFRESH_SLOT = 20;

	private static String[][] PRESETS = {
			{  null		 , null        },
			{ "LPOSX"	 , "LPOSY"     },
			{ "VISIONX"	 , "VISIONY"   },
			{ "SLAMPX"   , "SLAMPY"    },
			{ "SPLPOSX"	 , "SPLPOSY"   },
			{ "GNDTRUTHX", "GNDTRUTHY" },
			{ "PRECLOCKX", "PRECLOCKY" },
			{ "GPSLX"    , "GPSLY"     },
			{ "LPOSRX"	 , "LPOSRY"    },

	};

	private final static String[] PRESET_NAMES = {
			"None",
			"Local Position",
			"Vision Position",
			"Planned path",
			"SP Loc.Position",
			"Ground truth",
			"Percision lock",
			"Local GPS Position",
			"Corr.Loc.Position",
	};

	private final static String[] SCALES = {
			"Auto", "0.1", "0.2", "0.5","1", "2", "5", "10", "20","50", "100", "200","500"
	};

	@FXML
	private SectionLineChart<Number,Number> xychart;

	@FXML
	private NumberAxis xAxis;

	@FXML
	private NumberAxis yAxis;

	@FXML
	private ChoiceBox<String> cseries1;

	@FXML
	private ChoiceBox<String> cseries2;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries1_x;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries1_y;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries2_x;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries2_y;

	@FXML
	private ChoiceBox<String> scale_select;

	@FXML
	private Slider rotation;

	@FXML
	private Label rot_label;

	@FXML
	private CheckBox normalize;

	@FXML
	private CheckBox auto_rotate;

	@FXML
	private Button export;

	@FXML
	private CheckBox force_zero;

	@FXML
	private CheckBox corr_zero;

	@FXML
	private CheckBox annotation;

	@FXML
	private CheckBox show_grid;

	@FXML
	private CheckBox show_edf;

	@FXML
	private CheckBox show_traj;

	private  XYChart.Series<Number,Number> series1;
	private  XYChart.Series<Number,Number> series2;

	private IMAVController control;

	private KeyFigureMetaData type1_x=null;
	private KeyFigureMetaData type1_y=null;

	private KeyFigureMetaData type2_x=null;
	private KeyFigureMetaData type2_y=null;

	private StateProperties state = null;

	private int resolution_ms 	= 50;
	private float scale     = 5;

	private final Preferences prefs = MAVPreferences.getInstance();

	private int current_x_pt=0;
	private int current_x0_pt=0;
	private int current_x1_pt=0;

	private int frame_secs =30;

	private float rotation_rad = 0;

	private final double[] p1 = new double[2];
	private final double[] p2 = new double[2];

	private final XYStatistics s1 = new XYStatistics();
	private final XYStatistics s2 = new XYStatistics();

	private XYDashBoardAnnotation dashboard1 = null;
	private XYDashBoardAnnotation dashboard2 = null;

	private PositionAnnotation    endPosition1 = null;
	private PositionAnnotation    endPosition2 = null;

	private XYSigmaAnnotation     sigma1 = null;
	private XYSigmaAnnotation     sigma2 = null;

	private XYGridAnnotation grid = null;
	private XYEDFAnnotation  edf = null;
	private XYSlamAnnotation slam = null;

	private XYTrajectoryAnnonation traj = null;

	private XYDataPool pool = null;

	private boolean refreshRequest = false;

	private XYMeasurement measurement;

	private double center_x, center_y;
	private double offset_x, offset_y;
	private double scale_rounding;
	private double scale_factor;

	private int id = 0;
	private long dashboard_update_tms  = 0;

	private final AnalysisDataModelMetaData meta  = AnalysisDataModelMetaData.getInstance();
	private final AnalysisModelService      dataService = AnalysisModelService.getInstance();

	private final BooleanProperty           isScrolling = new SimpleBooleanProperty();
	private final FloatProperty             replay      = new SimpleFloatProperty(0);
	private final IntegerProperty           timeFrame   = new SimpleIntegerProperty(30);
	private final FloatProperty             scroll      = new SimpleFloatProperty(0);

	private final WorkQueue wq = WorkQueue.getInstance();

	public XYChartWidget() {

		this.state = StateProperties.getInstance();
		this.pool = new XYDataPool();

		FXMLLoadHelper.load(this, "XYChartWidget.fxml");

		dataService.registerListener(this);

	}

	@Override
	public void update(long now) {
		if(isVisible() && !isDisabled()) {
			updateGraph(refreshRequest,0);
		}
	}

	@FXML
	private void initialize() {

		this.grid = new XYGridAnnotation();
		this.edf  = new XYEDFAnnotation();
		this.slam = new XYSlamAnnotation(Color.DARKSLATEBLUE);

		this.traj = new XYTrajectoryAnnonation(Color.BROWN);

		this.sigma1 = new XYSigmaAnnotation(Color.AZURE);
		this.sigma2 = new XYSigmaAnnotation(Color.LINEN);

		this.dashboard1 = new XYDashBoardAnnotation(0,s1);
		this.dashboard2 = new XYDashBoardAnnotation(90,s2);

		this.endPosition1 = new PositionAnnotation("P",Color.DARKSLATEBLUE);
		this.endPosition2 = new PositionAnnotation("P",Color.DARKOLIVEGREEN);

		final Group chartArea = (Group)xychart.getAnnotationArea();
		//		final Rectangle zoom = new Rectangle();
		//		zoom.setStrokeWidth(0);
		//		chartArea.getChildren().add(zoom);
		//		zoom.setFill(Color.color(0,0.6,1.0,0.1));
		//		zoom.setVisible(false);
		//		zoom.setY(0);

		xychart.setAnimated(false);

		ContextMenu contextMenu = new ContextMenu();
		MenuItem imageCopy = new MenuItem("Copy graph to clipboard");
		imageCopy.setOnAction((e) -> copyToClipboardImage());
		contextMenu.getItems().add(imageCopy);
		xychart.setOnContextMenuRequested((event) -> {
			contextMenu.show(xychart, event.getScreenX(), event.getScreenY());

		});

		measurement = new XYMeasurement((Group)xychart.getPlotArea());

		xychart.setOnMouseClicked(click -> {
			if (click.getClickCount() == 2) {
				force_zero.setSelected(true);
				try {
					setScaling(Float.parseFloat(scale_select.getValue()));
				} catch(Exception e) { setScaling(0); };
			//	updateGraph(true,0);
			} else {
				if(dataService.isCollecting()) {
					// set target if collecting
					if(control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.INTERACTIVE)) {
						Point2D mouseSceneCoords = new Point2D(click.getSceneX(), click.getSceneY());
						float x = xAxis.getValueForDisplay(xAxis.sceneToLocal(mouseSceneCoords).getY()).floatValue();
						float y = yAxis.getValueForDisplay(yAxis.sceneToLocal(mouseSceneCoords).getX()).floatValue();
						msg_msp_command msp = new msg_msp_command(255,1);
						msp.command = MSP_CMD.MSP_CMD_OFFBOARD_SETLOCALPOS;
						msp.param1 =  x;
						msp.param2 =  y;
						msp.param3 =  Float.NaN;
						msp.param4 =  Float.NaN;
						control.sendMAVLinkMessage(msp);
					}
				} else {
					if(click.getButton().compareTo(MouseButton.SECONDARY)==0)
						this.copyToClipboardImage();
				}
			}
			click.consume();
		});

		xychart.setOnMousePressed(mouseEvent -> {

			if(dataService.isCollecting() || dataService.isReplaying()) {
				mouseEvent.consume();
				return;
			}
			measurement.start(mouseEvent.getSceneX()- chartArea.getLocalToSceneTransform().getTx(), mouseEvent.getSceneY()-chartArea.getLocalToSceneTransform().getTy());
		});

		xychart.setOnMouseReleased(mouseEvent -> {
			if(dataService.isCollecting() || dataService.isReplaying()) {
				mouseEvent.consume();
				return;
			}
			measurement.end();
		});

		xychart.setOnMouseDragged(event -> {
			if(dataService.isCollecting() || dataService.isReplaying()) {
				return;
			}
			measurement.measure(event.getSceneX()- chartArea.getLocalToSceneTransform().getTx(), event.getSceneY()-chartArea.getLocalToSceneTransform().getTy());
		});

		xychart.setOnScroll(event -> {

			force_zero.setSelected(false);
			center_x += event.getDeltaY() * scale / 600.0 ;
			center_y -= event.getDeltaX() * scale / 600.0 ;
			//	event.consume();
			setScaling(scale);
			updateGraph(false,0);
		});

		//		xychart.setOnRotate(new EventHandler<RotateEvent>() {
		//			@Override public void handle(RotateEvent event) {
		//				System.err.println("Rectangle: Rotate event" +
		//						", inertia: " + event.isInertia() +
		//						", direct: " + event.isDirect() +
		//						", Angle: " + event.getAngle()
		//						);
		//				event.consume();
		//			}
		//		});
		//
		//		xychart.setOnRotationStarted(new EventHandler<RotateEvent>() {
		//			@Override public void handle(RotateEvent event) {
		//				System.err.println("Rectangle: Rotate event started");
		//				event.consume();
		//			}
		//		});
		//
		//		xychart.setOnRotationFinished(new EventHandler<RotateEvent>() {
		//			@Override public void handle(RotateEvent event) {
		//				System.err.println("Rectangle: Rotate event finished");
		//				event.consume();
		//			}
		//		});


		xAxis.setAutoRanging(true);
		xAxis.setForceZeroInRange(false);
		yAxis.setAutoRanging(true);
		yAxis.setForceZeroInRange(false);


		cseries1.getItems().addAll(PRESET_NAMES);
		cseries2.getItems().addAll(PRESET_NAMES);

		xychart.setLegendVisible(false);
		//		xychart.prefWidthProperty().bind(widthProperty().subtract(20));
		//		xychart.prefHeightProperty().bind(heightProperty().subtract(20));

		initKeyFigureSelection(meta.getKeyFigures());

		scale_select.getItems().addAll(SCALES);
		scale_select.getSelectionModel().select(0);

		setScaling(5);

		xAxis.setTickUnit(1); yAxis.setTickUnit(1);

		xychart.heightProperty().addListener((e,o,n) -> {
			setScaling(scale);
			Platform.runLater(() -> {
				updateGraph(true,0);
			});
		});

		xychart.widthProperty().addListener((e,o,n) -> {
			setScaling(scale);
			Platform.runLater(() -> {
				updateGraph(true,0);
			});
		});

		xychart.prefHeightProperty().bind(heightProperty().subtract(10));

		cseries1.setOnMouseClicked((e) -> {
			if(cseries1.getSelectionModel().getSelectedIndex() == 0) {
				cseries1_x.getSelectionModel().select(0);
				cseries1_y.getSelectionModel().select(0);
			}
		});

		cseries1.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				if(PRESETS[newValue.intValue()][0]!=null)
					cseries1_x.getSelectionModel().select(meta.getMetaData(PRESETS[newValue.intValue()][0]));
				else
					cseries1_x.getSelectionModel().select(0);

				if(PRESETS[newValue.intValue()][1]!=null)
					cseries1_y.getSelectionModel().select(meta.getMetaData(PRESETS[newValue.intValue()][1]));
				else
					cseries1_y.getSelectionModel().select(0);

				prefs.putInt(MAVPreferences.XYCHART_FIG_1,newValue.intValue());

			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				if(PRESETS[newValue.intValue()][0]!=null)
					cseries2_x.getSelectionModel().select(meta.getMetaData(PRESETS[newValue.intValue()][0]));
				else
					cseries2_x.getSelectionModel().select(0);

				if(PRESETS[newValue.intValue()][1]!=null)
					cseries2_y.getSelectionModel().select(meta.getMetaData(PRESETS[newValue.intValue()][1]));
				else
					cseries2_y.getSelectionModel().select(0);

				prefs.putInt(MAVPreferences.XYCHART_FIG_2,newValue.intValue());
			}

		});

		cseries2.setOnMouseClicked((e) -> {
			if(cseries2.getSelectionModel().getSelectedIndex() == 0) {
				cseries2_x.getSelectionModel().select(0);
				cseries2_y.getSelectionModel().select(0);
			}
		});

		cseries1_x.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			String x_desc = "";
			if(nv!=null) {
				type1_x = nv;

				if(type1_x.hash!=0)
					x_desc = x_desc + type1_x.desc1+" ["+type1_x.uom+"] ";
				if(type2_x.hash!=0)
					x_desc = x_desc + type2_x.desc1+" ["+type2_x.uom+"] ";
				xAxis.setLabel(x_desc);


				updateRequest();
			}
		});


		cseries1_y.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			String y_desc = "";
			if(nv!=null) {
				type1_y = nv;

				if(type1_y.hash!=0)
					y_desc = y_desc + type1_y.desc1+" ["+type1_y.uom+"] ";
				if(type2_y.hash!=0)
					y_desc = y_desc + type2_y.desc1+" ["+type2_y.uom+"] ";
				yAxis.setLabel(y_desc);

				updateRequest();
			}
		});

		cseries2_x.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			String x_desc = "";
			if(nv!=null) {
				type2_x = nv;
				if(type1_x.hash!=0)
					x_desc = x_desc + type1_x.desc1+" ["+type1_x.uom+"] ";
				if(type2_x.hash!=0)
					x_desc = x_desc + type2_x.desc1+" ["+type2_x.uom+"] ";
				xAxis.setLabel(x_desc);

				corr_zero.setDisable(!(type1_y.hash!=0 && (type2_y.hash!=0)));

				updateRequest();
			}
		});


		cseries2_y.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			String y_desc = "";
			if(nv!=null) {
				type2_y = nv;
				if(type1_y.hash!=0)
					y_desc = y_desc + type1_y.desc1+" ["+type1_y.uom+"] ";
				if(type2_y.hash!=0)
					y_desc = y_desc + type2_y.desc1+" ["+type2_y.uom+"] ";
				yAxis.setLabel(y_desc);

				corr_zero.setDisable(!(type1_y.hash!=0 && (type2_y.hash!=0)));

				updateRequest();
			}
		});

		cseries1.getSelectionModel().select(prefs.getInt(MAVPreferences.XYCHART_FIG_1,0));
		cseries2.getSelectionModel().select(prefs.getInt(MAVPreferences.XYCHART_FIG_2,0));


		scale_select.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if(newValue.intValue()>0)
					scale = Float.parseFloat(SCALES[newValue.intValue()]);
				else
					scale = 0;
				setScaling(scale);
				updateRequest();
				prefs.putInt(MAVPreferences.XYCHART_SCALE,newValue.intValue());
			}

		});

		xychart.setOnZoom(event -> {
			scale = (float)(scale / event.getZoomFactor());
			setScaling(scale);
		});

		rotation.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				auto_rotate.setSelected(false);
				rotation_rad = MSPMathUtils.toRad(new_val.intValue());
				rot_label.setText("Rotation: ["+new_val.intValue()+"°]");
				updateRequest();

			}
		});

		rotation.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					rotation_rad = 0;
					rotation.setValue(0);
					rot_label.setText("Rotation: [ 0°]");
				}
			}
		});

		rotation.setTooltip(new Tooltip("Double-click to set to 0°"));

		auto_rotate.setDisable(true);
		auto_rotate.selectedProperty().addListener((v,ov,nv) -> {
			rotation.setDisable(nv.booleanValue());
			rotation.setValue(0);
			updateRequest();
		});


		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
		});

		timeFrame.addListener((v, ov, nv) -> {
			setXResolution(nv.intValue());
		});

		force_zero.selectedProperty().addListener((e,o,n) -> {
			if(!n.booleanValue()) {
				//		center_x = 0; center_y=0;
				setScaling(scale);
			} else
				updateRequest();
			prefs.putBoolean(MAVPreferences.XYCHART_CENTER,n.booleanValue());
		});

		force_zero.setSelected(prefs.getBoolean(MAVPreferences.XYCHART_CENTER, false));

		corr_zero.setOnAction((ActionEvent event)-> {
			updateRequest();
			prefs.putBoolean(MAVPreferences.XYCHART_OFFSET,corr_zero.isSelected());
		});

		corr_zero.setSelected(prefs.getBoolean(MAVPreferences.XYCHART_OFFSET, false));

		scroll.addListener((v, ov, nv) -> {
			int x1 =  dataService.calculateIndexByFactor(nv.floatValue())+1;	
			if(x1 < (timeFrame.get() * 1000 / dataService.getCollectorInterval_ms())) {
				current_x1_pt = x1;
				current_x0_pt = 0;
				updateGraph(true,x1);
			}	 
			else {
				current_x0_pt = dataService.calculateX0Index(x1);
				updateGraph(true,0);
			}
		});

		replay.addListener((v, ov, nv) -> {
			//			if(isDisabled())
			//				return;
			//			refreshRequest = true; 
			//			if(nv.intValue()<=5) {
			//				current_x0_pt =  0;
			//				Platform.runLater(() -> updateGraph(true, 1) );
			//			} else
			//				Platform.runLater(() -> updateGraph(false,nv.intValue()) );
			//			dataService.setCurrent(nv.intValue());
			if(isDisabled())
				return;

			if(nv.intValue()<0) {
				current_x0_pt =  dataService.calculateX0Index(-nv.intValue());
				if(current_x0_pt>0)
					current_x_pt =  dataService.calculateX1Index(-nv.intValue());
				else {
					current_x_pt = -nv.intValue();
				}

				dataService.setCurrent(-nv.intValue());
				updateGraph(true,  -nv.intValue());
			} else {
				updateGraph(false,  nv.intValue());
				dataService.setCurrent(nv.intValue());
			}
		});

		annotation.selectedProperty().addListener((v, ov, nv) -> {
			updateRequest();
		});

		annotation.selectedProperty().set(true);

		// TODO: handle grid annotation properly
		//		show_grid.setDisable(true);


		show_grid.selectedProperty().addListener((v, ov, nv) -> {
			if(nv.booleanValue()) {
				//		grid.invalidate(true);
				rotation_rad = 0;
				rotation.setValue(0);

			} 
			rotation.setDisable(nv.booleanValue());

			updateRequest();
			prefs.putBoolean(MAVPreferences.XYCHART_SLAM,show_grid.isSelected());
		});

		show_edf.selectedProperty().addListener((v, ov, nv) -> {
			if(nv.booleanValue()) {
				//		grid.invalidate(true);
				rotation_rad = 0;
				rotation.setValue(0);
			}

			rotation.setDisable(nv.booleanValue());

			updateRequest();
			prefs.putBoolean(MAVPreferences.XYCHART_EDF,show_edf.isSelected());
		});


		show_traj.selectedProperty().addListener((v, ov, nv) -> {      
			updateRequest();
			prefs.putBoolean(MAVPreferences.XYCHART_TRAJ,show_traj.isSelected());
		});


		show_traj.setSelected(prefs.getBoolean(MAVPreferences.XYCHART_TRAJ, false));
		show_grid.setSelected(prefs.getBoolean(MAVPreferences.XYCHART_SLAM, false));
		show_edf.setSelected(prefs.getBoolean(MAVPreferences.XYCHART_EDF, false));
		rotation.setDisable(show_grid.isSelected());
		rotation.setDisable(show_edf.isSelected());


		//
		//		this.disabledProperty().addListener((l,o,n) -> {
		//			if(!n.booleanValue() && !state.getReplayingProperty().get()) {
		//				Platform.runLater(() -> {
		//					grid.clear(); slam.clear();
		//					grid.setModel(control.getCurrentModel());
		//					updateRequest();
		//				});
		//			}
		//		});
	}

	private void setXResolution(int frame) {
		this.frame_secs = frame;
		if(frame >= 200)
			resolution_ms = 100;
		else if(frame >= 60)
			resolution_ms = 40;
		else if(frame >= 30)
			resolution_ms = 20;
		else if(frame >= 15)
			resolution_ms = 20;
		else
			resolution_ms = dataService.getCollectorInterval_ms();

		current_x0_pt = dataService.calculateX0IndexByFactor(1);
		current_x_pt  = dataService.calculateX0IndexByFactor(1);
		scroll.setValue(1);
		refreshChart();
	}

	public void saveAsPng(String path) {
		SnapshotParameters param = new SnapshotParameters();
		System.out.println(MAVPreferences.isLightTheme());
		if(!MAVPreferences.isLightTheme())
			param.setFill(Color.BLACK);
		WritableImage image = xychart.snapshot(param, null);
		File file = new File(path+"/xychart.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}

	public void copyToClipboardImage() {

		final SnapshotParameters param = new SnapshotParameters();
		if(!MAVPreferences.isLightTheme())
			param.setFill(Color.BLACK);

		WritableImage snapshot = xychart.snapshot(param, null);
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();

		content.putImage(snapshot);
		clipboard.setContent(content);

	}

	public void setKeyFigureSelection(KeyFigurePreset preset) {

		Platform.runLater(() -> {
			if(preset!=null) {
				cseries1.getSelectionModel().select(0);
				cseries2.getSelectionModel().select(0);

				setKeyFigure(cseries1_x,preset.getKeyFigure(0));
				setKeyFigure(cseries1_y,preset.getKeyFigure(1));
				setKeyFigure(cseries2_x,preset.getKeyFigure(2));
				setKeyFigure(cseries2_y,preset.getKeyFigure(3));

				updateGraph(true,0);
			}
		});
	}

	public KeyFigurePreset getKeyFigureSelection() {
		KeyFigurePreset preset = new KeyFigurePreset(id,0,0,type1_x.hash,type1_y.hash,type2_x.hash, type2_y.hash);
		return preset;
	}

	@Override
	public FloatProperty getReplayProperty() {
		return replay;
	}


	private void updateGraph(boolean refresh, int max_x0) {

		AnalysisDataModel m =null, m0=null; int max_x  = 0; long slot_tms;


		if(isDisabled() || series1==null || series2==null) {
			refreshRequest = false;
			return;
		}

		if(auto_rotate.isSelected()) {
			rotation_rad = -control.getCurrentModel().attitude.y;
		}

		List<AnalysisDataModel> mList = dataService.getModelList();
		if(mList==null) {
			return;
		}

		if(refresh) {

			grid.invalidate(isLocalPositionSelected(type1_x.hash,type1_y.hash));
			edf.invalidate(isLocalPositionSelected(type1_x.hash,type1_y.hash));

			if(mList.size()==0 && dataService.isCollecting()) {
				refreshRequest = true; return;
			}

			if(dataService.getModelList().size()==0) {
				if(series1.getData().size()>0)
					series1.getData().remove(0,series1.getData().size()-1);
				if(series2.getData().size()>0)
					series2.getData().remove(0,series2.getData().size()-1);
			} else {
				series1.getData().clear();
				series2.getData().clear();
			}
			pool.invalidateAll();
			synchronized(this) {
				xychart.getData().clear();
				xychart.getData().add(series1);
				xychart.getData().add(series2);
			}

			xychart.getAnnotations().clearAnnotations(Layer.FOREGROUND);
			xychart.getAnnotations().clearAnnotations(Layer.BACKGROUND);


			if(mList.size()>0 && isLocalPositionSelected(type1_x.hash,type1_y.hash)) {
				xychart.getAnnotations().add(slam, Layer.FOREGROUND);
				endPosition1.setVisible(false);
				sigma1.setVisible(false);
			} else {
				slam.clear();
				endPosition1.setVisible(true);
				sigma1.setVisible(true);
			}


			if(show_grid.isSelected() &&  mList.size()>0 && isLocalPositionSelected(type1_x.hash,type1_y.hash)) {
				xychart.getAnnotations().add(grid,Layer.BACKGROUND);
				slam.enableSLAM(true);
			} else {
				slam.enableSLAM(false);
				grid.clear();
			}

			if(show_edf.isSelected() &&  mList.size()>0 && isLocalPositionSelected(type1_x.hash,type1_y.hash)) {
				xychart.getAnnotations().add(edf,Layer.BACKGROUND);
				slam.enableSLAM(true);
			} else {
				slam.enableSLAM(false);
				edf.clear();
			}

			if(show_traj.isSelected() &&  mList.size()>0 && isLocalPositionSelected(type1_x.hash,type1_y.hash)) {
				xychart.getAnnotations().add(traj, Layer.BACKGROUND);
				traj.refresh();
			} else {
				traj.clear();
			}

			s1.setKeyFigures(type1_x, type1_y);
			if(type1_x.hash!=0 && type1_y.hash!=0 && annotation.isSelected() && mList.size()>0)  {
				m = mList.get(0);
				rotateRad(p1,m.getValue(type1_x), m.getValue(type1_y),
						rotation_rad);

				xychart.getAnnotations().add(dashboard1, Layer.FOREGROUND);
				xychart.getAnnotations().add(endPosition1, Layer.FOREGROUND);
				xychart.getAnnotations().add(
						new PositionAnnotation(p1[0],p1[1],"S", Color.DARKSLATEBLUE)
						,Layer.FOREGROUND);

				xychart.getAnnotations().add(sigma1, Layer.FOREGROUND);
			}

			s2.setKeyFigures(type2_x, type2_y);
			if(type2_x.hash!=0 && type2_y.hash!=0 && annotation.isSelected() && mList.size()>0)  {
				if(corr_zero.isSelected() && type1_x.hash!=0 && type1_y.hash!=0 ) {
					rotateRad(p2,m.getValue(type1_x), 
							m.getValue(type1_y),
							rotation_rad);
				}
				else {
					offset_x = 0;
					offset_y = 0;
					if(m!=null)
						rotateRad(p2,m.getValue(type2_x), m.getValue(type2_y), rotation_rad);
				}

				xychart.getAnnotations().add(dashboard2, Layer.FOREGROUND);
				xychart.getAnnotations().add(endPosition2, Layer.FOREGROUND);
				xychart.getAnnotations().add(
						new PositionAnnotation(p2[0],p2[1],"S", Color.DARKOLIVEGREEN)
						,Layer.FOREGROUND);

				xychart.getAnnotations().add(sigma2, Layer.FOREGROUND);

			}

			if(state.getRecordingProperty().get()==AnalysisModelService.STOPPED ) {
				current_x_pt = current_x0_pt;
				current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / dataService.getCollectorInterval_ms();

			} else {
				current_x0_pt = dataService.calculateX0IndexByFactor(1);
				current_x_pt  = dataService.calculateX0IndexByFactor(1);	
			}

			if(current_x_pt < 0) current_x_pt = 0;
		}

		if(mList.size()<1) {
			return;
		}

		if((force_zero.isSelected() || annotation.isSelected()) &&  (System.currentTimeMillis()-dashboard_update_tms) > 200) {
			wq.addSingleTask("LP",() -> {
				dashboard_update_tms = System.currentTimeMillis();
				s1.getStatistics(current_x0_pt,current_x1_pt,mList);
				s2.getStatistics(current_x0_pt,current_x1_pt,mList);
			});
			
			if(show_edf.isSelected())
				edf.update();
			if(show_grid.isSelected())
				grid.update();
		}

		if(force_zero.isSelected() && scale > 0 ) {


			double x = 0; double y = 0;

			scale_factor = Math.round(scale * xychart.getWidth()/xychart.getHeight()*scale_rounding ) /scale_rounding;


			if(type1_x.hash!=0) {
				//		if(type1_x.hash!=0 && type2_x.hash==0) {
				x = s1.center_x;
				y = s1.center_y;
			}

			if(type2_x.hash!=0 && type1_x.hash==0)	{
				x = s2.center_x;
				y = s2.center_y;
			}

			if(Math.abs(x - center_x)> scale/4) {
				x = (float)(Math.round(x * scale_rounding ) /scale_rounding);
				xAxis.setLowerBound(x-scale);
				xAxis.setUpperBound(x+scale);
				center_x = x;
				traj.refresh();
			}
			if(Math.abs(y - center_y)> scale/4) {
				//		y = (int)(y *  100) / (100f);
				y = (float)(Math.round(y * scale_rounding ) /scale_rounding);
				//System.out.println(scale_factor);
				yAxis.setLowerBound(y-scale_factor);
				yAxis.setUpperBound(y+scale_factor);
				center_y = y;
				traj.refresh();
			}

		}

		if(corr_zero.isSelected() && type2_x.hash!=0 && type2_y.hash!=0) {
			// Search for an valid initial m0 within the first 200ms for offset correction
			for(int i=0; i< 10 && i < mList.size();i++) {
				m0 = mList.get(i);
				if(mList.get(i).isValid(type2_x) && mList.get(i).isValid(type2_y)) {
					m0 = mList.get(i);
					break;
				} 
			}
		}

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			if(state.getRecordingProperty().get()==AnalysisModelService.STOPPED ) {
				if(max_x0 > 0)
					max_x = max_x0 < mList.size() ?  max_x0 : mList.size();
				else
					max_x = current_x1_pt < mList.size() ?  current_x1_pt : mList.size()  ;
			} else
				max_x = mList.size();


			slot_tms = System.currentTimeMillis();

			((XYObservableListWrapper<?>)series1.getData()).begin();
			((XYObservableListWrapper<?>)series2.getData()).begin();

			while(current_x_pt<max_x && ((System.currentTimeMillis()-slot_tms) < REFRESH_SLOT || refreshRequest)) {
				//	System.out.println(current_x_pt+"<"+max_x+":"+resolution_ms+" --> "+dataService.getCollectorInterval_ms());
				if(((current_x_pt * dataService.getCollectorInterval_ms()) % resolution_ms) == 0) {

					m = mList.get(current_x_pt);


					if(series1.getData().size()>0 ||series2.getData().size()>0) {
						slam.setModel(m); traj.setModel(m);
					}

					if(current_x_pt > current_x1_pt) {

						current_x0_pt += resolution_ms / dataService.getCollectorInterval_ms();
						current_x1_pt += resolution_ms / dataService.getCollectorInterval_ms();

						if(series1.getData().size()>0) {
							pool.invalidate(series1.getData().get(0));
							series1.getData().remove(0);
						}
						if(series2.getData().size()>0) {
							pool.invalidate(series2.getData().get(0));
							series2.getData().remove(0);
						}
					}

					if(type1_x.hash!=0 && type1_y.hash!=0) {
						rotateRad(p1,m.getValue(type1_x), m.getValue(type1_y),
								rotation_rad);
						series1.getData().add(pool.checkOut(p1[0],p1[1]));
						endPosition1.setPosition(p1[0], p1[1]);
					}

					if(type2_x.hash!=0 && type2_y.hash!=0) {
						if(corr_zero.isSelected() && type1_x.hash!=0 && type1_y.hash!=0  && m0 != null) {

							rotateRad(p2,m.getValue(type2_x)-(m0.getValue(type2_x) - m0.getValue(type1_x)), 
									m.getValue(type2_y)-(m0.getValue(type2_y) - m0.getValue(type1_y)),
									rotation_rad);

						}
						else
							rotateRad(p2,m.getValue(type2_x), m.getValue(type2_y),
									rotation_rad);
						series2.getData().add(pool.checkOut(p2[0],p2[1]));
						endPosition2.setPosition(p2[0], p2[1]);
					}
				}

				current_x_pt++;
			}

			((XYObservableListWrapper<?>)series1.getData()).end();
			((XYObservableListWrapper<?>)series2.getData()).end();

			sigma1.setPosition(p1[0], p1[1],s1.stddev_xy);
			sigma2.setPosition(p2[0], p2[1],s2.stddev_xy);

		}
		refreshRequest = false;
	}


	public XYChartWidget setup(IMAVController control) {
		series1 = new XYChart.Series<Number,Number>(XYCollections.<Data<Number,Number>>observableArrayList());

		xychart.getData().add(series1);
		series2 = new XYChart.Series<Number,Number>(XYCollections.<Data<Number,Number>>observableArrayList());
		xychart.getData().add(series2);

		this.control = control;

		grid.setController(control);
		edf.setController(control);

		state.getRecordingProperty().addListener((o,ov,nv) -> {
			if(nv.intValue()!=AnalysisModelService.STOPPED) {
				setXResolution(timeFrame.get());
				traj.clear();
			}
		});

		setXResolution(30);

		state.getLogLoadedProperty().addListener((o,ov,nv) -> Platform.runLater(() -> updateRequest()));

		current_x0_pt = dataService.calculateX0IndexByFactor(1);
		current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / dataService.getCollectorInterval_ms();

		scale_select.getSelectionModel().select(prefs.getInt(MAVPreferences.XYCHART_SCALE,0));
		try {
			scale = Float.parseFloat(scale_select.getSelectionModel().getSelectedItem());
			setScaling(scale);
		} catch(NumberFormatException e) {

		}

		// Used to update the grid when connected.
		//		ExecutorService.get().scheduleAtFixedRate(() -> {
		//			if(state.getRecordingProperty().get()==0 && !isDisabled()) {
		//				updateRequest();
		//			}
		//		}, 0, 1, TimeUnit.SECONDS);

		this.disabledProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
				wq.addSingleTask("LP",100, () -> {
					Platform.runLater(() -> {
						if(!state.getReplayingProperty().get()) {
							if(state.getRecordingProperty().get() == AnalysisModelService.STOPPED) {
								int x1 =  dataService.calculateIndexByFactor(scroll.get());	
								current_x0_pt = dataService.calculateX0Index(x1);
							}
							updateRequest();
						} else {
							updateGraph(true,replay.intValue());
						}
					});
				});
			}
		});

		state.getRecordingProperty().addListener((e,o,n) -> {
			if(o.intValue() != AnalysisModelService.COLLECTING && n.intValue() == AnalysisModelService.COLLECTING 
					&& !this.getParent().disabledProperty().get()) {
				setScaling(scale);
				updateRequest();
			}
		});

		return this;
	}

	public IntegerProperty getTimeFrameProperty() {
		return timeFrame;
	}

	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}

	public BooleanProperty getIsScrollingProperty() {
		return isScrolling ;
	}

	@Override
	public void refreshChart() {
		if(frame_secs > 60)
			frame_secs = 60;
		current_x0_pt = dataService.calculateX0Index(1);
		setScaling(scale);
		Platform.runLater(() -> updateRequest());
	}


	private void updateRequest() {
		if(!refreshRequest) {
			center_x = 0; center_y = 0;
			refreshRequest = true;
			if(!state.getReplayingProperty().get()) {
				updateGraph(refreshRequest,0);
			} else {
				updateGraph(refreshRequest,replay.intValue());
			}
		}
	}


	private void setScaling(float scale) {

		this.scale = scale;

		if(scale > 1 && scale < 20)
			this.grid.setScale(scale);

		if(scale>0) {

			force_zero.setDisable(false);
			xAxis.setAutoRanging(false);
			yAxis.setAutoRanging(false);

			if(scale>100) {
				xAxis.setTickUnit(50); yAxis.setTickUnit(50);
			} else if(scale>10) {
				xAxis.setTickUnit(10); yAxis.setTickUnit(10);
			} else if(scale>2) {
				xAxis.setTickUnit(1); yAxis.setTickUnit(1);
			} else if(scale>1f) {
				xAxis.setTickUnit(0.5); yAxis.setTickUnit(0.5);
			} else if(scale>0.5f) {
				xAxis.setTickUnit(0.1); yAxis.setTickUnit(0.1);
			} else {
				xAxis.setTickUnit(0.05); yAxis.setTickUnit(0.05);
			}

			scale_rounding = 1/yAxis.getTickUnit();
			scale_factor = Math.round(scale * xychart.getWidth()/xychart.getHeight()*scale_rounding ) /scale_rounding;

			xAxis.setLowerBound(center_x-scale);
			xAxis.setUpperBound(center_x+scale);
			yAxis.setLowerBound(center_y-scale_factor);
			yAxis.setUpperBound(center_y+scale_factor);

		} else {
			xAxis.setAutoRanging(true);
			yAxis.setAutoRanging(true);
			force_zero.setDisable(true);
			force_zero.setSelected(false);
		}
		traj.refresh();

	}

	private  void rotateRad(double[] rotated, double posx, double posy, double heading_rad) {
		if(heading_rad!=0) {
			rotated[1] =  ( posx - center_x ) * Math.cos(heading_rad) +
					( posy - center_y ) * Math.sin(heading_rad) + center_x;
			rotated[0] = -( posx - center_x ) * Math.sin(heading_rad) +
					( posy - center_y ) * Math.cos(heading_rad) + center_y;
		} else {
			rotated[1] = posx;
			rotated[0] = posy;
		}
	}

	private void setKeyFigure(ChoiceBox<KeyFigureMetaData> series,int keyFigureHash) {
		KeyFigureMetaData v = meta.getKeyFigureMap().get(keyFigureHash);
		if(v!=null) {
			series.getSelectionModel().select(v);
		} else {
			series.getSelectionModel().select(0);
		}
	}

	private boolean isLocalPositionSelected(int hashx, int hashy) {
		return meta.getMetaData(PRESETS[1][0]).hash == hashx && meta.getMetaData(PRESETS[1][1]).hash == hashy;
	}

	private void initKeyFigureSelection(List<KeyFigureMetaData> kfl) {

		cseries1_x.getItems().clear();
		cseries2_x.getItems().clear();
		cseries1_y.getItems().clear();
		cseries2_y.getItems().clear();

		type1_x = new KeyFigureMetaData();
		type2_x = new KeyFigureMetaData();
		type1_y = new KeyFigureMetaData();
		type2_y = new KeyFigureMetaData();

		cseries1_x.getItems().add((type1_x));
		cseries1_x.getItems().addAll(kfl);
		cseries1_y.getItems().add((type1_y));
		cseries1_y.getItems().addAll(kfl);
		cseries1_x.getSelectionModel().select(0);
		cseries1_y.getSelectionModel().select(0);

		cseries2_x.getItems().add((type2_x));
		cseries2_x.getItems().addAll(kfl);
		cseries2_y.getItems().add((type1_y));
		cseries2_y.getItems().addAll(kfl);
		cseries2_x.getSelectionModel().select(0);
		cseries2_y.getSelectionModel().select(0);

		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);

		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);
	}

	private class XYMeasurement {

		private double centerx, centery;
		private Group group;
		private final Line       line  = new Line();
		private final Circle    start  = new Circle(2);
		private final Label zoom_label = new Label();
		private final Pane       pane  = new Pane();

		public XYMeasurement(Group g) {
			this.pane.setVisible(false);
			this.group = g;
			this.start.setStroke(Color.web("#1c6478").darker());
			this.start.setFill(Color.web("#1c6478").brighter());
			this.line.setStroke(Color.web("#1c6478").brighter());
			if(MAVPreferences.isLightTheme()) 
				this.zoom_label.setStyle("-fx-font-size: 8pt;-fx-text-fill: #202020;");
			else
				this.zoom_label.setStyle("-fx-font-size: 8pt;-fx-text-fill: #FFFFFF;");
			this.pane.getChildren().addAll( start, zoom_label, line);
			this.group.getChildren().add(pane);
		}

		public void start(double x, double y) {
			this.zoom_label.setVisible(false);
			centerx = x;
			centery = y;
			this.start.setCenterX(centerx);
			this.start.setCenterY(centery);
			this.line.setEndX(centerx);
			this.line.setEndY(centery);
			this.line.setStartX(centerx);
			this.line.setStartY(centery);
			this.pane.setVisible(true);

		}

		public void end() {
			this.pane.setVisible(false);
			this.zoom_label.setVisible(false);
		}

		public void measure (double x, double y) {
			double radius   = Math.sqrt((x-centerx)*(x-centerx)+(y-centery)*(y-centery));
			double distance = (xAxis.getUpperBound())-xAxis.getValueForDisplay(radius).doubleValue();
			this.line.setEndX(x);
			this.line.setEndY(y);
			if(radius > 0.05) {
				this.zoom_label.setVisible(true);
				this.zoom_label.setLayoutX(x+12);
				this.zoom_label.setLayoutY(y+12);
				this.zoom_label.setText(String.format("%#.3f m",distance));	
			} else
				this.zoom_label.setVisible(false);
		}

	}
}
