package com.comino.flight.widgets.fx.controls;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class Badge extends Label {

	private final String DEFAULT_CSS = "-fx-alignment: center;-fx-border-radius: 3;-fx-background-radius: 3;-fx-padding: 2;";

	public final static int MODE_OFF 		=  0;
	public final static int MODE_ON 		=  1;
	public final static int MODE_BLINK  	=  2;
	public final static int MODE_ERROR  	=  3;

	private int     mode   = MODE_OFF;
	private String  color  = null;
	private String  textColor   = null;
	private boolean toggle = false;
	private Timeline timeline = null;

	public Badge() {
		super();
		this.setDisable(false);
		this.setPrefWidth(999);
		this.color   = "#"+Integer.toHexString(Color.DARKGRAY.hashCode());
		setStyle(DEFAULT_CSS+"-fx-background-color: #404040;-fx-text-fill:#808080;");
	}

	public void setMode(int mode, Color color) {
		this.mode = mode;
		setBackgroundColor(color);
	}

	public void setMode(int mode) {

		if(timeline!=null)
		  timeline.stop();
		toggle=false;
		setDisable(toggle);

		switch(mode) {
		case MODE_OFF:
			setStyle(DEFAULT_CSS+"-fx-background-color: #404040;-fx-text-fill:#808080;");
			break;
		case MODE_ON:
			setStyle(DEFAULT_CSS+"-fx-background-color:"+color+";-fx-text-fill:"+textColor+";");
			break;
		case MODE_BLINK:
			setStyle(DEFAULT_CSS+"-fx-background-color:"+color+";-fx-text-fill:#F0F0F0;");
			if(timeline!=null) timeline.play();
			break;
		case MODE_ERROR:
			setStyle(DEFAULT_CSS+"-fx-background-color: #804040;-fx-text-fill:#F0F0F0;");
			if(timeline!=null) timeline.play();
			break;
		default:
			setStyle(DEFAULT_CSS+"-fx-background-color: #404040;-fx-text-fill:#808080;");
			break;
		}
		this.mode = mode;
	}


	public void setBackgroundColor(Color color) {
		this.color = "#"+Integer.toHexString(color.darker().desaturate().hashCode());
		if(color.getBrightness()<0.7)
			this.textColor ="#F0F0F0";
		else
			this.textColor ="#"+Integer.toHexString(color.darker().darker().darker().darker().hashCode());
		setMode(mode);
	}

	public void setRate(String rate) {
		timeline = new Timeline(new KeyFrame(
				Duration.millis(Integer.parseInt(rate)),
				ae -> {
					toggle = !toggle; setDisable(toggle);
				}));
		timeline.setCycleCount(Animation.INDEFINITE);
	}

	public String getRate() {
		return null;
	}

	public String getColor() {
		return color.toString();
	}

	public void setColor(String value) {
		setBackgroundColor(Color.valueOf(value));
	}
}
