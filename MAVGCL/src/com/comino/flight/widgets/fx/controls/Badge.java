package com.comino.flight.widgets.fx.controls;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
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
		init(Color.DARKGRAY,300);
	}

	public void init(Color color, int rate_ms) {
		this.setPrefWidth(999);
		this.color   = "#"+Integer.toHexString(color.hashCode());
		setStyle(DEFAULT_CSS+"-fx-background-color: #404040;-fx-text-fill:#808080;");
		timeline = new Timeline(new KeyFrame(
				Duration.millis(rate_ms),
				ae -> {
					toggle = !toggle; setDisable(toggle);
				}));
		timeline.setCycleCount(Animation.INDEFINITE);
	}

	public void setMode(int mode) {

		if(this.mode==mode)
			return;

		timeline.stop();

		switch(mode) {
		case MODE_OFF:
			setStyle(DEFAULT_CSS+"-fx-background-color: #404040;-fx-text-fill:#808080;");
			break;
		case MODE_ON:
			setStyle(DEFAULT_CSS+"-fx-background-color:"+color+";-fx-text-fill:"+textColor+";");
			break;
		case MODE_BLINK:
			setStyle(DEFAULT_CSS+"-fx-background-color:"+color+";-fx-text-fill:#F0F0F0;");
			timeline.play(); break;
		case MODE_ERROR:
			setStyle(DEFAULT_CSS+"-fx-background-color: #904040;-fx-text-fill:#F0F0F0;");
			break;
		default:
			setStyle(DEFAULT_CSS+"-fx-background-color: #404040;-fx-text-fill:#808080;");
			break;
		}
		this.mode = mode;
	}

	public String getColor() {
		return color.toString();
	}

	public void setColor(String value) {
		Color c = Color.valueOf(value);
		this.color = "#"+Integer.toHexString(c.darker().desaturate().desaturate().hashCode());
		if(c.getBrightness()<0.7)
			this.textColor ="#"+Integer.toHexString(c.brighter().brighter().hashCode());
		else
			this.textColor ="#"+Integer.toHexString(c.darker().darker().hashCode());
	}
}
