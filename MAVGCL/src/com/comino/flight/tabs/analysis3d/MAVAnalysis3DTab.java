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

package com.comino.flight.tabs.analysis3d;

import java.io.IOException;
import java.util.List;

import org.fxyz.cameras.CameraTransformer;

import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.fxyz.ext.FlightCubeViewer;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.MSTYPE;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

public class MAVAnalysis3DTab extends BorderPane  implements IChartControl {


	private static int COLLECTOR_CYCLE = 50;
	private static int REFRESH_MS = 300;

	private Task<Integer> task;

	private PerspectiveCamera camera;
	private final double sceneWidth = 1000;
	private final double sceneHeight = 700;


	private CameraTransformer cameraTransform = new CameraTransformer();

	private double mousePosX;
	private double mousePosY;
	private double mouseOldX;
	private double mouseOldY;
	private double mouseDeltaX;
	private double mouseDeltaY;

	@FXML
	private SubScene scene;

	private FlightCubeViewer cubeViewer;

	private IMAVController control;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private DoubleProperty scroll        = new SimpleDoubleProperty(0);

	private int resolution_ms 	= 50;


	private int current_x_pt=0;
	private int current_x0_pt=0;
	private int current_x1_pt=0;



	public MAVAnalysis3DTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVAnalysis3DTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Task<Integer>() {

			@Override
			protected Integer call() throws Exception {
				while(true) {
					try {
						Thread.sleep(REFRESH_MS);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					if(!isCollecting.get() && control.getCollector().isCollecting()) {
						current_x_pt = 0;
						scroll.setValue(0);
						updateGraph(true);
					}

					isCollecting.set(control.getCollector().isCollecting());


					if(isCollecting.get() && control.isConnected())
						updateValue(control.getCollector().getModelList().size());



				}
				return control.getCollector().getModelList().size();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Integer>() {
			@Override
			public void changed(ObservableValue<? extends Integer> observableValue, Integer oldData, Integer newData) {
				updateGraph(false);

			}
		});

		setOnMousePressed((MouseEvent me) -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
		});
		setOnMouseDragged((MouseEvent me) -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = (mousePosX - mouseOldX);
			mouseDeltaY = (mousePosY - mouseOldY);

			double modifier = 10.0;
			double modifierFactor = 0.5;

			if (me.isControlDown()) {
				modifier = 0.5;
			}
			if (me.isShiftDown()) {
				modifier = 50.0;
			}
			if (me.isPrimaryButtonDown() && !me.isAltDown()) {
				cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 0.2) % 360 + 540) % 360 - 180);  // +
				cameraTransform.rx.setAngle(((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 0.2) % 360 + 540) % 360 - 180);  // -
				cubeViewer.adjustPanelsByPos(cameraTransform.rx.getAngle(), cameraTransform.ry.getAngle(), cameraTransform.rz.getAngle());
			} else if (me.isSecondaryButtonDown() || ( me.isPrimaryButtonDown() && me.isAltDown())) {
				double z = camera.getTranslateZ();
				double newZ = z + mouseDeltaY * modifierFactor * modifier;
				camera.setTranslateZ(newZ);
			} else if (me.isMiddleButtonDown()) {
				cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3);  // -
				cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3);  // -
			}
		});


	}


	@FXML
	private void initialize() {

		Group sceneRoot = new Group();
		SubScene scene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
		scene.setFill(Color.BLACK);
		//Setup camera and scatterplot cubeviewer
		camera = new PerspectiveCamera(true);
		cubeViewer = new FlightCubeViewer(2000,100, false);


		sceneRoot.getChildren().addAll(cubeViewer);
		//setup camera transform for rotational support
		cubeViewer.getChildren().add(cameraTransform);
		cameraTransform.setTranslate(0, 0, 0);
		cameraTransform.getChildren().add(camera);
		camera.setNearClip(0.1);
		camera.setFarClip(30000.0);
		camera.setTranslateZ(-4000);
		cameraTransform.ry.setAngle(-23.0);
		cameraTransform.rx.setAngle(-28.0);
		cubeViewer.adjustPanelsByPos(cameraTransform.rx.getAngle(), cameraTransform.ry.getAngle(), cameraTransform.rz.getAngle());
		//add a Point Light for better viewing of the grid coordinate system
		PointLight light = new PointLight(Color.WHITE);
		cameraTransform.getChildren().add(light);
		light.setTranslateX(camera.getTranslateX());
		light.setTranslateY(camera.getTranslateY());
		light.setTranslateZ(camera.getTranslateZ());
		scene.setCamera(camera);

		cubeViewer.scatterRadius=15;



		this.setCenter(scene);

		timeFrame.addListener((v, ov, nv) -> {
			setXResolution(nv.intValue());
		});

		scroll.addListener((v, ov, nv) -> {

			if(!isCollecting.get()) {
				current_x0_pt = (int)(
						( control.getCollector().getModelList().size()  - timeFrame.get() *  1000f / COLLECTOR_CYCLE)
						* (1 - nv.intValue() / 100f))	;
				if(current_x0_pt<0)
					current_x0_pt = 0;
				if(!disabledProperty().get())
					updateGraph(true);
			}
		});

		this.disabledProperty().addListener((v, ov, nv) -> {
			if(ov.booleanValue() && !nv.booleanValue()) {
				current_x_pt = 0;
				scroll.setValue(0);
				refreshChart();
			}
		});


	}



	public MAVAnalysis3DTab setup(ChartControlWidget recordControl, IMAVController control) {
		this.control = control;
		recordControl.addChart(this);
		ExecutorService.get().execute(task);
		return this;
	}


	private double old_x;
	private double old_y;
	private double old_z;
	private int frame_secs = 30;


	private void setXResolution(int frame) {
		this.current_x_pt = 0;
		this.frame_secs  = frame;

		if(frame > 600)
			resolution_ms = 500;
		else if(frame > 200)
			resolution_ms = 300;
		else if(frame > 30)
			resolution_ms = 200;
		else if(frame > 20)
			resolution_ms = 100;
		else
			resolution_ms = 50;

		scroll.setValue(0);
		refreshChart();

	}



	private void updateGraph(boolean refresh) {

		if(refresh) {
			synchronized(this) {

				cubeViewer.clear();
			}

			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

			if(current_x_pt < 0) current_x_pt = 0;
		}

		List<DataModel> mList = control.getCollector().getModelList();

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			int max_x = mList.size();
			if(!isCollecting.get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			while(current_x_pt<max_x) {


				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0) {

					current_x0_pt += resolution_ms / COLLECTOR_CYCLE;
					current_x1_pt += resolution_ms / COLLECTOR_CYCLE;


					double x = 500.0 * MSTYPE.getValue(mList.get(current_x_pt),MSTYPE.MSP_RNEDX);
					double z = (250.0 * MSTYPE.getValue(mList.get(current_x_pt),MSTYPE.MSP_RNEDZ))+999.0;
					double y = 500.0 * MSTYPE.getValue(mList.get(current_x_pt),MSTYPE.MSP_RNEDY);


					if(Math.abs(x-old_x)>20 || Math.abs(y-old_y)>20 || Math.abs(z-old_z)>20) {
						old_x = x; old_y = y; old_z = z;


						synchronized(this) {
							cubeViewer.addData(x,z,y);
						}

						if(current_x_pt > current_x1_pt) {

							if(cubeViewer.getxAxisData().size()>0)
								cubeViewer.remove(0);
						}
					}

				}

				current_x_pt++;
			}
		}
	}


	public BooleanProperty getCollectingProperty() {
		return isCollecting;
	}

	public IntegerProperty getTimeFrameProperty() {
		return timeFrame;
	}

	@Override
	public DoubleProperty getScrollProperty() {
		return scroll;
	}

	@Override
	public void refreshChart() {
		current_x0_pt = control.getCollector().getModelList().size() - frame_secs * 1000 / COLLECTOR_CYCLE;
		if(current_x0_pt < 0)
			current_x0_pt = 0;
		updateGraph(true);
	}

}


