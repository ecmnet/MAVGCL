package com.comino.flight.widgets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;

public class LEDControl extends Circle implements Runnable {

	public static int MODE_OFF 		=  0;
	public static int MODE_ON 		=  1;
	public static int MODE_BLINK  	=  2;

	private int     mode   = MODE_OFF;
	private Color   color  = null;
	private boolean toggle = false;
	private int     rate_ms = 100;

	public void init(Color color, int rate_ms) {
		this.color = color;
		this.rate_ms = rate_ms;
		this.setFill(Color.LIGHTGRAY);
		this.setRadius(8);
		this.setStrokeType(StrokeType.INSIDE);
	}


	public void setMode(int mode) {
		this.mode = mode;
		switch(mode) {
		   case 0:
			   setFill(Color.LIGHTGRAY); break;
		   case 1:
			   setFill(color); break;
		   case 2:
			   setFill(color); new Thread(this).start(); break;
		}
	}

	@Override
	public void run() {
		while(mode == MODE_BLINK ) {
			try {
				Thread.sleep(rate_ms);
			} catch (InterruptedException e) { }
			toggle = !toggle;
			if(toggle)
				setFill(color);
			else
				setFill(Color.LIGHTGRAY);
		}
		switch(mode) {
		   case 0:
			   setFill(Color.LIGHTGRAY); break;
		   case 1:
			   setFill(color); break;
		}
	}
}
