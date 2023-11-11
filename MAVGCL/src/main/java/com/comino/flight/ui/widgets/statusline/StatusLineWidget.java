/****************************************************************************
 *
 *   Copyright (c) 2017,2022 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.ui.widgets.statusline;

import java.io.IOException;
import java.util.List;

import org.mavlink.messages.ESTIMATOR_STATUS_FLAGS;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.file.FileHandler;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.flight.ui.widgets.panel.ChartControlWidget;
import com.comino.jfx.extensions.Badge;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.control.impl.MAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.model.segment.Vision;
import com.comino.mavcom.param.PX4Parameters;
import com.comino.mavutils.MSPMathUtils;

import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class StatusLineWidget extends Pane implements IChartControl {

	@FXML
	private HBox hbox;

	@FXML
	private Badge ready;

	@FXML
	private Badge driver;

	@FXML
	private Badge time;

	@FXML
	private Badge mode;

	@FXML
	private Badge rc;

	@FXML
	private Badge bat;

	@FXML
	private Badge controller;

	@FXML
	private Badge gps;

	@FXML
	private Badge ekf;

	@FXML
	private Badge vision;

	@FXML
	private Badge gpos;

	@FXML
	private Badge lpos;

	@FXML
	private Badge home;

	@FXML
	private Badge wp;

	@FXML
	private Badge locked;


	private IMAVController control;

	private FloatProperty scroll              = new SimpleFloatProperty(0);
	private FloatProperty replay              = new SimpleFloatProperty(0);
	private BooleanProperty isScrolling       = new SimpleBooleanProperty();

	private AnalysisModelService collector = AnalysisModelService.getInstance();
	private StateProperties state = null;
	private String filename;

	private final static String[]  EKF2STATUS_TEXTS = { "", "ATT", "RPOS", "APOS", "FAULT", "VEL", "OTHER"  };

	private AnimationTimer task = null;

	int current_x0_pt = 0; int current_x1_pt = 0;
	long last = 0;

	private DataModel msp_model;
	private AnalysisDataModel model;

	public StatusLineWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatusLineWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new AnimationTimer() {


			@Override
			public void handle(long now) {

				if((now - last) < 200_000_000)
					return;
				last = now;

				int ekf_status = getEKF2Status();
				List<AnalysisDataModel> list = null;

				if(msp_model.slam.wpcount > 0) {
					wp.setText(String.format("WP %d", msp_model.slam.wpcount));
					wp.setMode(Badge.MODE_ON);
				}
				else if(msp_model.sys.t_takeoff_ms < 0 ) {
					wp.setText(String.format("T % d", (int)(msp_model.sys.t_takeoff_ms/1000-0.5f)));
					wp.setMode(Badge.MODE_ON);
				} 
				else if(!msp_model.sys.isStatus(Status.MSP_ARMED)){
					wp.setText("WP");
					wp.setMode(Badge.MODE_OFF);
				}

				if(!control.isConnected() || !msp_model.sys.isSensorAvailable(Status.MSP_GPS_AVAILABILITY) || !hasGPS()) {
					gps.setMode(Badge.MODE_OFF);
					gps.setText("GPS");
				}
				else {
					switch(msp_model.gps.fixtype & 0xF) {

					case 2:
						gps.setMode(Badge.MODE_ON);
						gps.setText("GPS");
					case 3:
						gps.setMode(Badge.MODE_ON);
						gps.setText("GPS fix");
						break;
					case 4:
						gps.setMode(Badge.MODE_ON);
						gps.setText("GPS 3D");
						break;
					case 5:
						gps.setMode(Badge.MODE_ON);
						gps.setText("DGPS");
						break;
					case 6:
						gps.setMode(Badge.MODE_ON);
						gps.setText("RTK float");
						break;
					case 7:
						gps.setMode(Badge.MODE_ON);
						gps.setText("RTK fixed");
						break;

					default:

						gps.setMode(Badge.MODE_OFF);
					}
				}


				filename = FileHandler.getInstance().getName();
				driver.setText(msp_model.sys.getSensorString());
				if(msp_model.vision.getShortText().length()>0)
					vision.setText(msp_model.vision.getShortText());

				if(control.isConnected()) {

					if(msp_model.home_state.g_lat!=0 && msp_model.home_state.g_lon!=0) 
						home.setMode(Badge.MODE_ON);
					else
						home.setMode(Badge.MODE_OFF);

					if(msp_model.sys.isSensorAvailable(Status.MSP_IMU_AVAILABILITY))

						driver.setMode(Badge.MODE_ON);

					if(msp_model.vision.isStatus(Vision.PUBLISHED))
						vision.setMode(Badge.MODE_ON);
					else
						vision.setMode(Badge.MODE_OFF);

					if(msp_model.sys.isSensorAvailable(Status.MSP_MSP_AVAILABILITY)) {

						if(msp_model.sys.isSensorAvailable(Status.MSP_FIDUCIAL_LOCKED)) {
							locked.setMode(Badge.MODE_ON);
							locked.setMode(Badge.MODE_SPECIAL);
						}
						else {
							locked.setMode(Badge.MODE_OFF);
						}

						if(msp_model.sys.isStatus(Status.MSP_READY_FOR_FLIGHT)) {
							ready.setMode(Badge.MODE_OK);
							ready.setText("READY");
						}
						else {
							ready.setMode(Badge.MODE_ERROR);
							ready.setText("NOT READY");
						}
					} else {

						ready.setMode(Badge.MODE_ON);
						if(ekf_status != 4) {
							ready.setMode(Badge.MODE_OFF);
							ready.setText("EKF2");

						} else {
							ready.setMode(Badge.MODE_ERROR);
							ready.setText("NOT READY");
						}
					}

					bat.setMode(Badge.MODE_ON);
					bat.setText(msp_model.sys.getBatTypeString());

				}
				else {
					ready.setMode(Badge.MODE_OFF);
					driver.setMode(Badge.MODE_OFF);
					ekf.setMode(Badge.MODE_OFF);
					vision.setMode(Badge.MODE_OFF);
					home.setMode(Badge.MODE_OFF);
					bat.setMode(Badge.MODE_OFF);
					bat.setText("UNKNOWN");
					driver.setText("COMPONENTS");
				}

				list = collector.getModelList();

				if(list.size()>0) {
					//					if(!state.getReplayingProperty().get() && !isScrolling.get()) {
					//						current_x0_pt = collector.calculateX0IndexByFactor(scroll.floatValue());
					//						current_x1_pt = collector.calculateX1IndexByFactor(scroll.floatValue());
					//					}

					if(state.getLogLoadedProperty().get() && current_x1_pt>1) {
						time.setText(
								String.format("%1$tM:%1$tS - %2$tM:%2$tS / %3$tM:%3$tS",
										(list.get(current_x0_pt).tms+list.get(0).tms)/1000,
										(list.get(current_x1_pt-1).tms+list.get(0).tms)/1000,
										(list.get(list.size()-1).tms+list.get(0).tms)/1000)
								);
					}
					else {
						current_x0_pt = collector.calculateX0IndexByFactor(1);
						current_x1_pt = collector.calculateX1IndexByFactor(1);
						if(current_x0_pt < list.size() && current_x1_pt>1 && current_x1_pt <= list.size()) {
							time.setText(
									String.format("%1$tM:%1$tS - %2$tM:%2$tS",
											(list.get(current_x0_pt).tms+list.get(0).tms)/1000,
											(list.get(current_x1_pt-1).tms+list.get(0).tms)/1000)
									);
						}
					}
					//					time.setBackgroundColor(Color.web("#1c6478"));
				} else {
					time.setText("00:00 - 00:00");
					//					time.setBackgroundColor(Color.GRAY);
				}

				if(state.getReplayingProperty().get()) {
					mode.setBackgroundColor(Color.web("#2989a3"));
					mode.setText("Replay");
					mode.setMode(Badge.MODE_ON);
				}
				else if(!filename.isEmpty()) {
					mode.setBackgroundColor(Color.web("#2989a3"));
					if(filename.indexOf(".") > 0)
						mode.setText(filename.substring(0,filename.indexOf(".")));
					else
						mode.setText(filename);
					mode.setMode(Badge.MODE_ON);
				}
				else if(control.isConnected()) {

					switch(control.getMode()) {
					case MAVController.MODE_NORMAL:
						mode.setText("Connected");
						break;
					case MAVController.MODE_USB:
						mode.setText("Serial");
						break;
					case MAVController.MODE_SITL:
						mode.setText("SITL");
						break;	
					case MAVController.MODE_SITL_PROXY:
						mode.setText("SITL Proxy");
						break;	
					}
					mode.setMode(Badge.MODE_ON);
				} else {
					mode.setMode(Badge.MODE_OFF);
				}

				if(list.size()==0) {
					time.setMode(Badge.MODE_OFF);
				} else {
					time.setMode(Badge.MODE_ON);
				}

				if(control.isConnected()) {
					ekf.setText(EKF2STATUS_TEXTS[ekf_status]);
					if(ekf_status != 4)
						ekf.setMode(Badge.MODE_ON);
					else
						ekf.setMode(Badge.MODE_ERROR);
				}



				if((msp_model.sys.isStatus(Status.MSP_RC_ATTACHED)))
					rc.setMode(Badge.MODE_ON);
				else
					rc.setMode(Badge.MODE_OFF);

				if((msp_model.sys.isStatus(Status.MSP_GPOS_VALID)))
					gpos.setMode(Badge.MODE_ON);
				else
					gpos.setMode(Badge.MODE_OFF);

				if((msp_model.sys.isStatus(Status.MSP_LPOS_VALID)))
					lpos.setMode(Badge.MODE_ON);
				else
					lpos.setMode(Badge.MODE_OFF);
			}

		};

		mode.setOnMouseClicked((e) -> {
			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(mode.getText());
			clipboard.setContent(content);
		});

		driver.setAlignment(Pos.CENTER_LEFT);
	}

	public void setup(IMAVController control) {
		ChartControlWidget.addChart(99,this);
		this.control = control;
		this.msp_model = control.getCurrentModel();
		this.model = AnalysisModelService.getInstance().getCurrent();
		this.state = StateProperties.getInstance();

		hbox.prefWidthProperty().bind(this.widthProperty().subtract(9));

		//	control.getStatusManager().addListener(Status.MSP_CONNECTED, (n) -> {
		//		state.getConnectedProperty().addListener((v,o,n) -> {
		//			
		//			driver.setDisable(!n.booleanValue());
		//			rc.setDisable(!n.booleanValue());
		//			gpos.setDisable(!n.booleanValue());
		//			lpos.setDisable(!n.booleanValue());
		//			controller.setDisable(!n.booleanValue());
		//			ekf.setDisable(!n.booleanValue());
		//			ready.setDisable(!n.booleanValue());
		//			
		//			if((msp_model.sys.isStatus(Status.MSP_GPOS_VALID)))
		//				gpos.setMode(Badge.MODE_ON);
		//			else
		//				gpos.setMode(Badge.MODE_OFF);
		//			
		//			if((msp_model.sys.isStatus(Status.MSP_LPOS_VALID)))
		//				lpos.setMode(Badge.MODE_ON);
		//			else
		//				lpos.setMode(Badge.MODE_OFF);
		//
		//		});

		state.getControllerConnectedProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				controller.setMode(Badge.MODE_ON);
			}
			else {
				controller.setMode(Badge.MODE_OFF);
			}

		});

		state.getLogLoadedProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				current_x1_pt = collector.calculateIndexByFactor(1);	
				current_x0_pt = collector.calculateX0Index(current_x1_pt);
			}
		});

		scroll.addListener((e,o,n) -> {
			current_x1_pt = collector.calculateIndexByFactor(n.floatValue());	
			current_x0_pt = collector.calculateX0Index(current_x1_pt);
		});


		replay.addListener((e,o,n) -> {
			if(n.intValue()>0) {
				current_x1_pt = n.intValue();
				current_x0_pt = collector.calculateX0Index(n.intValue());
			}
		});

		ready.setOnMouseClicked((event) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_CHECK_READY;
			control.sendMAVLinkMessage(msp);
		});

		task.start();

	}

	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}

	@Override
	public IntegerProperty getTimeFrameProperty() {
		return null;
	}

	public BooleanProperty getIsScrollingProperty() {
		return isScrolling;
	}

	@Override
	public FloatProperty getReplayProperty() {
		return replay;
	}

	@Override
	public void refreshChart() {

	}

	public void setKeyFigureSelection(KeyFigurePreset preset) {

	}

	public KeyFigurePreset getKeyFigureSelection() {
		return null;
	}

	private boolean hasGPS() {
		MAVGCLPX4Parameters params = MAVGCLPX4Parameters.getInstance();
		return params.getParam("SYS_HAS_GPS")!=null && params.getParam("SYS_HAS_GPS").value == 1;
	}

	private int getEKF2Status() {
		int flags = (int)msp_model.est.flags;


		if(flags == 0
				|| (flags & ESTIMATOR_STATUS_FLAGS.ESTIMATOR_ACCEL_ERROR)==ESTIMATOR_STATUS_FLAGS.ESTIMATOR_ACCEL_ERROR
				|| (flags & ESTIMATOR_STATUS_FLAGS.ESTIMATOR_ATTITUDE)==0)  {
			return 4;
		}

		if((flags & ESTIMATOR_STATUS_FLAGS.ESTIMATOR_POS_HORIZ_ABS)==ESTIMATOR_STATUS_FLAGS.ESTIMATOR_POS_HORIZ_ABS )
			return 3;
		else if((flags & ESTIMATOR_STATUS_FLAGS.ESTIMATOR_PRED_POS_HORIZ_ABS)==ESTIMATOR_STATUS_FLAGS.ESTIMATOR_PRED_POS_HORIZ_ABS )
			return 3;
		else if ((flags & ESTIMATOR_STATUS_FLAGS.ESTIMATOR_POS_HORIZ_REL)==ESTIMATOR_STATUS_FLAGS.ESTIMATOR_POS_HORIZ_REL )
			return 2;
		else if ((flags & ESTIMATOR_STATUS_FLAGS.ESTIMATOR_PRED_POS_HORIZ_REL)==ESTIMATOR_STATUS_FLAGS.ESTIMATOR_PRED_POS_HORIZ_REL )
			return 2;
		else if ((flags & ESTIMATOR_STATUS_FLAGS.ESTIMATOR_VELOCITY_HORIZ)==ESTIMATOR_STATUS_FLAGS.ESTIMATOR_VELOCITY_HORIZ )
			return 5;
		else if ((flags & ESTIMATOR_STATUS_FLAGS.ESTIMATOR_ATTITUDE)==ESTIMATOR_STATUS_FLAGS.ESTIMATOR_ATTITUDE )
			return 1;
		else
			return 6;

	}


}
