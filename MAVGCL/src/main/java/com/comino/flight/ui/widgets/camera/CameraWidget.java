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

package com.comino.flight.ui.widgets.camera;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.ui.panel.control.FlightControlPanel;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.flight.ui.widgets.panel.ControlWidget;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.model.segment.Status;
import com.comino.video.src.player.VideoPlayer;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXML;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

public class CameraWidget extends ChartControlPane implements IChartControl {

	private int X = 640 ;
	private int Y = 360 ;


	@FXML
	private ImageView         image;
	private VideoPlayer       player;


	private boolean			big_size=false;
	private FloatProperty  	scroll= new SimpleFloatProperty(0);
	private FloatProperty  	replay= new SimpleFloatProperty(0);

	private ControlWidget   widget;

	public CameraWidget() {
		super();
		FXMLLoadHelper.load(this, "CameraWidget.fxml");
		image.fitWidthProperty().bind(this.widthProperty());
		image.fitHeightProperty().bind(this.heightProperty());
	}



	@FXML
	private void initialize() {

		this.setFixedRatio((double)Y/X);

		fadeProperty().addListener((observable, oldvalue, newvalue) -> {
			player.show(newvalue.booleanValue());	
			setAsBackground(state.getVideoAsBackgroundProperty().get());
		});

		image.setOnMouseClicked(event -> {

			if(event.getButton().compareTo(MouseButton.SECONDARY)==0) {
				if(!big_size)
					big_size=true;
				else
					big_size=false;
				resize(big_size,X,Y);

			} else {

				if(event.getClickCount()!=2)
					return;

				if(state.getStreamProperty().get() == 0)
					state.getStreamProperty().set(1);
				else
					state.getStreamProperty().set(0);	

			}
			event.consume();
		});

		state.getVideoAsBackgroundProperty().addListener((o,ov,nv) -> {
			setAsBackground(nv.booleanValue());
		});


		state.getStreamProperty().addListener((o,ov,nv) -> {

			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.SELECT_VIDEO_STREAM;
			msp.param1  = nv.intValue();
			control.sendMAVLinkMessage(msp);
			var player = VideoPlayer.getInstance();
			if(player!=null)
				player.changeStreamSource(state.getStreamProperty().intValue());
		});

		state.getRecordingProperty().addListener((o,ov,nv) -> {

//			if(nv.intValue()!=AnalysisModelService.STOPPED) {
//				if(player.recording(true)) 
//					logger.writeLocalMsg("[mgc] MP4 recording started", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
//
//			} else {
//				if(player.recording(false))
//					logger.writeLocalMsg("[mgc] MP4 recording stopped", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
//			}
		});

		scroll.addListener((v, ov, nv) -> {
			player.playAt(nv.floatValue());
		});

		replay.addListener((v, ov, nv) -> {
			player.playAtIndex(nv.intValue());
		});

	}

	public FloatProperty getScrollProperty() {
		return scroll;
	}

	private void resize(boolean big, int maxX, int maxY) {
		Platform.runLater(() -> {
			if(big) {
				this.setInitialHeight(maxY); 
				this.setInitialWidth(maxX);
				this.setMaxHeight(maxY);
				this.setMaxWidth(maxX);
			} else
			{
				this.setInitialHeight(maxY/2); this.setMaxHeight(maxY/2);
				this.setInitialWidth(maxX/2); this.setMaxWidth(maxX/2);
			}
		});
	}

	public void setup(IMAVController control,FlightControlPanel flightControl) {

		this.control = control;
		this.widget  = flightControl.getControl();
		this.player  = VideoPlayer.getInstance(control,image,true);

		ChartControlPane.addChart(91,this);

		state.getVideoStreamAvailableProperty().addListener((e,o,n) -> {

			if(state.getLogLoadedProperty().get() || state.getReplayingProperty().get())
				return;

			if(n.booleanValue())  {
				if(widget.getVideoVisibility().get() && player.isConnected()) 
					player.reconnect();
			} 
		});


	}

	private void setAsBackground(boolean enable) {
		Platform.runLater(() -> {
		if(enable) {
			this.toBack();
			this.setLayoutX(-300); this.setLayoutY(-200);
			this.setWidth(1920); this.setHeight(1440);
			image.setEffect(new ColorAdjust(0,-0.5,-0.6,-0.2));
		} else {
			this.toFront();
			setMoveable(true);
			resetSize();
			image.setEffect(null);
		}
	  });
	}

	@Override
	protected void perform_action() {
		System.out.println("Disable video");
		widget.getVideoVisibility().setValue(false);
	}       

	@Override
	public IntegerProperty getTimeFrameProperty() {
		return null;
	}

	@Override
	public FloatProperty getReplayProperty() {
		return replay;
	}

	@Override
	public BooleanProperty getIsScrollingProperty() {
		return null;
	}

	@Override
	public void refreshChart() {
		player.playAtCurrent();
	}

	@Override
	public KeyFigurePreset getKeyFigureSelection() {
		return null;
	}

	@Override
	public void setKeyFigureSelection(KeyFigurePreset preset) {

	}
}
