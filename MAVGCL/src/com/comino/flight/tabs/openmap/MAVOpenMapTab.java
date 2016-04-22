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

package com.comino.flight.tabs.openmap;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.lodgon.openmapfx.core.DefaultBaseMapProvider;
import org.lodgon.openmapfx.core.LayeredMap;
import org.lodgon.openmapfx.core.LicenceLayer;
import org.lodgon.openmapfx.core.PositionLayer;
import org.lodgon.openmapfx.providers.BingTileProvider;

import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.flight.widgets.gps.details.GPSDetailsWidget;
import com.comino.mav.control.IMAVController;
import com.comino.model.file.FileHandler;
import com.comino.model.file.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.collector.ModelCollectorService;
import com.comino.openmapfx.ext.CanvasLayer;
import com.comino.openmapfx.ext.CanvasLayerPaintListener;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MAVOpenMapTab extends BorderPane  implements IChartControl {

	private final static int MAP_UPDATE_MS = 100;

	private final static String[] GPS_SOURCES = { "Global Position", "Raw GPS data" };


	private final static MSTYPE TYPES[][] =
		{ { MSTYPE.MSP_GLOBPLAT, MSTYPE.MSP_GLOBPLON },
				{ MSTYPE.MSP_RAW_GPSLAT, MSTYPE.MSP_RAW_GPSLON } };

	@FXML
	private BorderPane mapviewpane;

	@FXML
	private Slider zoom;

	@FXML
	private ChoiceBox<String> gpssource;

	@FXML
	private CheckBox viewdetails;

	@FXML
	private CheckBox mapfollow;

	@FXML
	private GPSDetailsWidget gpsdetails;

	@FXML
	private Button export;

	private LayeredMap map;

	private PositionLayer 		positionLayer;
	private PositionLayer 		homeLayer;
	private LicenceLayer  		licenceLayer;
	private CanvasLayer			canvasLayer;

	private Task<Long> task;

	private DataModel model;
	private int type = 0;


	private int index=0;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);

	private FloatProperty  scroll       = new SimpleFloatProperty(0);


	private ModelCollectorService collector;

	private IMAVController control;

	public MAVOpenMapTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVOpenMapTab.fxml"));
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
						Thread.sleep(100);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled() ) {
						break;
					}

					if(!isCollecting.get() && collector.isCollecting()) {
						canvasLayer.redraw(true);
					}
					isCollecting.set(collector.isCollecting());

					Platform.runLater(() -> {
						try {
							if(model.gps.ref_lat!=0 && model.gps.ref_lon!=0) {
								//map.setCenter(model.gps.ref_lat, model.gps.ref_lon);
								homeLayer.setVisible(true);
								homeLayer.updatePosition(model.gps.ref_lat, model.gps.ref_lon);
							} else
								homeLayer.setVisible(false);

							if(model.gps.numsat>3) {

								if(mapfollow.selectedProperty().get()) {
									map.setCenter(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]));
									canvasLayer.redraw(true);
								} else {
									canvasLayer.redraw(false);
								}
								positionLayer.updatePosition(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]),model.attitude.h);
							}

						} catch(Exception e) { e.printStackTrace(); }
					});
				}
				return System.currentTimeMillis();
			}
		};
	}


	@FXML
	private void initialize() {


		gpsdetails.setVisible(false);
		gpsdetails.fadeProperty().bind(viewdetails.selectedProperty());

		mapfollow.selectedProperty().set(true);

		// TODO 1.0: provide application directory

		String mapFileName = FileHandler.getInstance().getBasePath()+"/MapCache";
		DefaultBaseMapProvider provider = new DefaultBaseMapProvider(new BingTileProvider("http://t0.tiles.virtualearth.net/tiles/a",mapFileName));

		gpssource.getItems().addAll(GPS_SOURCES);
		gpssource.getSelectionModel().select(0);
		type = 0;

		map = new LayeredMap(provider);

		mapviewpane.setCenter(map);

		Rectangle clip = new Rectangle();
		mapviewpane.setClip(clip);
		clip.heightProperty().bind(map.heightProperty());
		clip.widthProperty().bind(mapviewpane.widthProperty());
		//		//		map.setCenter(49.142899,11.577723);
		map.setZoom(19.5);

		canvasLayer = new CanvasLayer();
		map.getLayers().add(canvasLayer);

		homeLayer = new PositionLayer(new Image(getClass().getResource("home.png").toString()));
		map.getLayers().add(homeLayer);

		positionLayer = new PositionLayer(new Image(getClass().getResource("airplane.png").toString()));
		map.getLayers().add(positionLayer);
		positionLayer.setVisible(true);


		positionLayer.updatePosition(49.142899,11.577723);

		licenceLayer = new LicenceLayer(provider);
		map.getLayers().add(licenceLayer);

		// Test paintlistener
		canvasLayer.addPaintListener(new CanvasLayerPaintListener() {

			Point2D p0; Point2D p1;  boolean first = true; DataModel m;


			@Override
			public void redraw(GraphicsContext gc, double width, double height, boolean refresh) {


				if(refresh) {
					index = 0;
					first = true;
				}

				// TODO 0.3: MAVOpenMapTab: Draw path also in replay

				if(isCollecting.get() &&
						(collector.getModelList().size()-index)>2*MAP_UPDATE_MS/collector.getCollectorInterval_ms()) {


					gc.setStroke(Color.DARKKHAKI); gc.setFill(Color.DARKKHAKI);
					gc.setLineWidth(2);
					for(int i=index; i<collector.getModelList().size();
							i += MAP_UPDATE_MS/collector.getCollectorInterval_ms()) {

						m = collector.getModelList().get(i);

						if(first) {
							p0 = map.getMapArea().getMapPoint(
									MSTYPE.getValue(m,TYPES[type][0]),MSTYPE.getValue(m,TYPES[type][1]));

							gc.fillOval(p0.getX()-4, p0.getY()-4,8,8);
							first = false; continue;
						}
						p1 = map.getMapArea().getMapPoint(
								MSTYPE.getValue(m,TYPES[type][0]),MSTYPE.getValue(m,TYPES[type][1]));

						gc.strokeLine(p0.getX(), p0.getY(), p1.getX(), p1.getY());
						p0 = map.getMapArea().getMapPoint(
								MSTYPE.getValue(m,TYPES[type][0]),MSTYPE.getValue(m,TYPES[type][1]));
					}
					index = collector.getModelList().size();
				}
			}

		});

		zoom.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				Platform.runLater(() -> {
					map.setZoom(zoom.getValue());
					canvasLayer.redraw(true);
				});
			}
		});

		zoom.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					zoom.setValue(19.5f);
					Platform.runLater(() -> {
						map.setZoom(zoom.getValue());
						canvasLayer.redraw(true);
					});
				}
			}
		});


		gpssource.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type = newValue.intValue();
				Platform.runLater(() -> {
					canvasLayer.redraw(true);
				});
			}

		});

		mapfollow.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(oldValue.booleanValue() && !newValue) {
					if(model.gps.ref_lat!=0)
						map.setCenter(model.gps.ref_lat, model.gps.ref_lon);
					else
						map.setCenter(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]));
					Platform.runLater(() -> {
						canvasLayer.redraw(true);
					});
				}
			}
		});


		scroll.addListener((v, ov, nv) -> {
			if(!isCollecting.get()) {

//				int current_x0_pt = (int)(
//						( collector.getModelList().size()-1)
//						- nv.intValue());
//
//				if(current_x0_pt<0)
//					current_x0_pt = 0;

				int current_x1_pt = control.getCollector().calculateX1Index(nv.floatValue());

				if(collector.getModelList().size()>0 && current_x1_pt > 0)
					model = collector.getModelList().get(current_x1_pt);
				else
					model = control.getCurrentModel();

			}
		});

		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
		});


		zoom.setTooltip(new Tooltip("Zooming"));
	}

	public void saveAsPng(String path) {
		SnapshotParameters param = new SnapshotParameters();
		param.setFill(Color.BLACK);
		WritableImage image = mapviewpane.snapshot(param, null);
		File file = new File(path+"/map.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}


	public MAVOpenMapTab setup(ChartControlWidget recordControl, IMAVController control) {
		this.collector = control.getCollector();
		this.model=control.getCurrentModel();
		this.control = control;

		gpsdetails.setup(control);
		recordControl.addChart(this);

		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
		return this;
	}


	public BooleanProperty getCollectingProperty() {
		return isCollecting;
	}

	public IntegerProperty getTimeFrameProperty() {
		return timeFrame;
	}


	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}


	@Override
	public void refreshChart() {

		if(collector.getModelList().size()>0)
			model = collector.getModelList().get(collector.getModelList().size()-1);
		else
			model = control.getCurrentModel();

		Platform.runLater(() -> {
			canvasLayer.redraw(true);
		});
	}

}
