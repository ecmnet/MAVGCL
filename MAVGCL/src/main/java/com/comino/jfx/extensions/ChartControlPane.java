/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.jfx.extensions;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.prefs.Preferences;

import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class ChartControlPane extends Pane {

	private static final int RESIZE_MARGIN = 25;

	protected IMAVController control = null;

	protected StateProperties state = null;
	protected Preferences     prefs = null;
	protected MSPLogger      logger = null;

	private FadeTransition in = null;
	private FadeTransition out = null;
	
	private double fixed_ratio = Double.NaN;

	private double mouseX ;
	private double mouseY ;

	private double initWidth;
	private double initHeight;

	private BooleanProperty fade       = new SimpleBooleanProperty(false);
	private BooleanProperty moveable   = new SimpleBooleanProperty(false);
	private BooleanProperty resizable  = new SimpleBooleanProperty(false);

	public boolean is_resizing = false;

	private String prefKey = MAVPreferences.CTRLPOS+this.getClass().getSimpleName().toUpperCase();

	protected static Map<Integer,IChartControl> charts = new HashMap<Integer,IChartControl>();

	public static void addChart(int id,IChartControl chart) {
		if(!charts.containsKey(id))
			charts.put(id,chart);
	}

	public ChartControlPane() {
		this(150);
	}

	public ChartControlPane(int duration_ms) {
		this(duration_ms,false);
	}

	public ChartControlPane(int duration_ms, boolean visible) {

		this.state  = StateProperties.getInstance();
		this.prefs  = MAVPreferences.getInstance();
		this.logger = MSPLogger.getInstance();

		in = new FadeTransition(Duration.millis(duration_ms), this);
		in.setFromValue(0.0);
		in.setToValue(1.0);

		out = new FadeTransition(Duration.millis(duration_ms), this);
		out.setFromValue(1.0);
		out.setToValue(0.0);

		setVisible(visible);
		fade.set(visible);

		out.setOnFinished(value -> {
			setVisible(false);
		});

		fade.addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(newValue.booleanValue()) {
					if(moveable.get()) {
						setLayoutX(MAVPreferences.getInstance().getDouble(prefKey+"X", 10));
						setLayoutY(MAVPreferences.getInstance().getDouble(prefKey+"Y", 10));
					}
					if(resizable.get()) {
						setWidth(MAVPreferences.getInstance().getDouble(prefKey+"SX", 320));
						setHeight(MAVPreferences.getInstance().getDouble(prefKey+"SY", 240));
					}
					setVisible(true);
					in.play();
				}
				else
					out.play();
			}
		});

		setOnMousePressed(event -> {

			initWidth = getWidth();
			initHeight = getHeight();

			if(moveable.get() || resizable.get()) {
				is_resizing = isResizeEvent(event);
				mouseX = event.getSceneX() ;
				mouseY = event.getSceneY() ;
			}
			event.consume();
		});

		setOnMouseDragged(event -> {

			final double deltaX = event.getSceneX() - mouseX;
			final double deltaY = event.getSceneY() - mouseY;

			if(resizable.get() && is_resizing) {
				if(!Double.isNaN(fixed_ratio)) {
					if(deltaX > deltaY) {
						this.setWidth(initWidth+deltaX);
						this.setHeight((initWidth+deltaX)*fixed_ratio);
					} else {
						this.setHeight(initHeight+deltaY);
						this.setWidth((initHeight+deltaY)/fixed_ratio);
					}
				}
				else {
				  this.setWidth(initWidth+deltaX);
				  this.setHeight(initHeight+deltaY);
				}
				event.consume();
				return;
			}	
			if(moveable.get()) {
				relocate(getLayoutX() + deltaX, getLayoutY() + deltaY);
				mouseX = event.getSceneX() ;
				mouseY = event.getSceneY() ;
				event.consume();
			}
		});

		setOnMouseReleased(event -> {
			is_resizing = false;
			relocate(getLayoutX(), getLayoutY());
			if(moveable.get()) {
				MAVPreferences.getInstance().putDouble(prefKey+"X",getLayoutX());
				MAVPreferences.getInstance().putDouble(prefKey+"Y",getLayoutY());
			}
			
			if(resizable.get()) {
				MAVPreferences.getInstance().putDouble(prefKey+"SX",getWidth());
				MAVPreferences.getInstance().putDouble(prefKey+"SY",getHeight());
			}
			this.setCursor(Cursor.DEFAULT);
			event.consume();
		});

		setOnMouseMoved(event -> {
			if(resizable.get()) {
				is_resizing = isResizeEvent(event);
			} else
				this.setCursor(Cursor.DEFAULT);
		});

	}
	
	public void setFixedRatio(double val) {
		this.fixed_ratio = val;
		this.setHeight(getWidth()*val);
	}

	public void setup(IMAVController control) {
		this.control = control;

	}

	public BooleanProperty fadeProperty() {
		return fade;
	}

	public BooleanProperty MoveableProperty() {
		return moveable;
	}

	public void setMoveable(boolean val) {
		setManaged(!val); moveable.set(val);
		if(moveable.get()) {
			setLayoutX(MAVPreferences.getInstance().getDouble(prefKey+"X", 10));
			setLayoutY(MAVPreferences.getInstance().getDouble(prefKey+"Y", 10));
		}
	}

	public boolean getMoveable() {
		return moveable.get();
	}

	public void setResizable(boolean val) {
		resizable.set(val);
	}

	public boolean getResizable() {
		return resizable.get();
	}

	public void refreshCharts() {
		for(Entry<Integer, IChartControl> chart : charts.entrySet()) {
			if(chart.getValue().getScrollProperty()!=null)
				chart.getValue().getScrollProperty().set(1);
			chart.getValue().refreshChart();
		}
	}

	public void setInitialWidth(double val) {
		setWidth(val);
	}

	public void setInitialHeight(double val) {
		setHeight(val);
	}

	public double getInitialWidth() {
		return initWidth;
	}

	public double getInitialHeight() {
		return initHeight;
	}

	private boolean isResizeEvent(MouseEvent event) {

		if(!resizable.get()) {
			setCursor(Cursor.DEFAULT);
			return false;
		}

		if(event.getY() > (getHeight() - RESIZE_MARGIN)) {
			if(event.getX() > (getWidth() - RESIZE_MARGIN)) {
				setCursor(Cursor.SE_RESIZE);
				return true;
			}
			//        	if(event.getX() < RESIZE_MARGIN) {
			//        		setCursor(Cursor.SW_RESIZE);
			//        		return true;
			//        	}
		} 
		//        if(event.getY() < RESIZE_MARGIN) {
		//        	if(event.getX() > (initWidth - RESIZE_MARGIN)) {
		//        		setCursor(Cursor.NE_RESIZE);
		//        		return true;
		//        	}
		//        	if(event.getX() < RESIZE_MARGIN) {
		//        		setCursor(Cursor.NW_RESIZE);
		//        		return true;
		//        	}
		//        }
		setCursor(Cursor.DEFAULT);
		return false;
	}


}
