package com.comino.flight.widgets.fx.controls;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;

public class LED extends GridPane implements Runnable {

	public final static int MODE_OFF 		=  0;
	public final static int MODE_ON 		=  1;
	public final static int MODE_BLINK  	=  2;
	public final static int MODE_ERROR  	=  3;

	private int     mode   = MODE_OFF;
	private Color   color  = null;
	private boolean toggle = false;
	private int     rate_ms = 0;

	private Circle circle = null;
	private Label  label  = null;

	public LED() {
		super();

		this.color = Color.RED;
		this.rate_ms = 300;

		setPadding(new Insets(3,0,3,0));
        setHgap(10);

		this.circle = new Circle(7);
		this.circle.setFill(Color.LIGHTGRAY);
		this.circle.setStrokeType(StrokeType.INSIDE);
		this.addColumn(0, circle);

		this.label  = new Label();
		this.addColumn(1, label);

	}

	public void init(Color color, int rate_ms) {
		this.color = color;
		this.rate_ms = rate_ms;
	}

	public String getText() {
        return label.getText();
    }

    public void setText(String value) {
        label.setText(value);
    }

    public String getColor() {
        return color.toString();
    }

    public void setColor(String value) {
       this.color = Color.valueOf(value);
    }


	public void setMode(int mode) {

		if(this.mode==mode)
			return;

		switch(mode) {
		case MODE_OFF:
			circle.setFill(Color.LIGHTGRAY); break;
		case MODE_ON:
			circle.setFill(color); break;
		case MODE_BLINK:
			circle.setFill(color); new Thread(this).start(); break;
		case MODE_ERROR:
			circle.setFill(Color.RED); break;
		}
		this.mode = mode;
	}


	@Override
	public void run() {
		while(mode == MODE_BLINK) {
			try {
				Thread.sleep(rate_ms);
			} catch (InterruptedException e) { }
			toggle = !toggle;
			if(toggle)
				circle.setFill(color);
			else
				circle.setFill(Color.LIGHTGRAY);
		}
		switch(mode) {
		case MODE_OFF:
			circle.setFill(Color.LIGHTGRAY); break;
		case MODE_ON:
			circle.setFill(color); break;
		}
	}
}
