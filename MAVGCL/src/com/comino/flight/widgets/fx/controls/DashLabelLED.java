package com.comino.flight.widgets.fx.controls;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

public class DashLabelLED extends GridPane {

	public final static int MODE_OFF 		=  0;
	public final static int MODE_ON 		=  1;
	public final static int MODE_BLINK  	=  2;

	private Line  line      = null;
	private Label label     = null;
	private Circle circle   = null;
	private Color color     = null;

	private int  mode = -1;
	private boolean toggle = false;
	private Timeline timeline;

	@SuppressWarnings("restriction")
	public DashLabelLED() {
		super();
		this.color = Color.WHITE;
		this.setPadding(new Insets(3,0,3,0));
		this.setHgap(4);

		label = new Label(); label.setTextFill(Color.DARKCYAN.brighter());
		line = new Line(); line.setStroke(Color.DARKCYAN.darker());

		this.addColumn(0, label);
		this.addColumn(1, line);
		this.setPrefWidth(999);

		this.circle = new Circle(4);
		this.circle.setFill(Color.TRANSPARENT);
		this.circle.setStrokeType(StrokeType.INSIDE);
		this.circle.setStroke(Color.ANTIQUEWHITE);
		this.addColumn(2, circle);

		final FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();

		this.widthProperty().addListener((v,ov,nv) -> {
			line.setStartX(0.0f);
			line.setStartY(this.prefHeightProperty().floatValue()/2);
			line.setEndX(this.getWidth()-fontLoader.computeStringWidth(label.getText(), label.getFont())-25);
			line.setEndY(this.prefHeightProperty().floatValue()/2);
		});
	}

	public DashLabelLED(String text) {
		this();
		label.setText(text);
	}

	public String getText() {
		return label.getText();
	}

	public void setText(String value) {
		label.setText(value);
	}

	public void setMode(int mode) {
		switch(mode) {
		case MODE_OFF:
			circle.setFill(Color.TRANSPARENT); break;
		case MODE_ON:
			circle.setFill(color); break;
		case MODE_BLINK:
			if(timeline!=null) timeline.play();
			break;
		}
		this.mode = mode;
	}

	public int getMode() {
		return mode;
	}

	public void setRate(String rate) {
		timeline = new Timeline(new KeyFrame(
				Duration.millis(Integer.parseInt(rate)),
				ae -> {
					if(toggle)
						circle.setFill(color);
					else
						circle.setFill(Color.TRANSPARENT);
					toggle = !toggle;
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
	       this.color = Color.valueOf(value);
	    }


}
