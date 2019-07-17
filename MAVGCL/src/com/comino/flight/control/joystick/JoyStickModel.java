package com.comino.flight.control.joystick;

import java.util.ArrayList;
import java.util.HashMap;

import net.java.games.input.Component;

public class JoyStickModel {

	public static  int   PRESSED   = 1;
	public static  int   RELEASED  = 0;

	private static int  THRESHOLD  = 5;
	private static int  TIMEOUT    = 500;

	public int throttle = 0;
	public int yaw      = 0;
	public int pitch    = 0;
	public int roll     = 0;

	private long tms    = 0;

	private HashMap<Integer,button>     buttons  = new HashMap<Integer,button>();
	private ArrayList<IControlListener> controls = new ArrayList<IControlListener>();

	public void addButtonListener(int button_index, IButtonListener listener ) {
		buttons.put(button_index,new button(listener));
	}

	public void addControlListener(IControlListener listener) {
		controls.add(listener);
	}


	public void scanButtons(Component[] co) {
		buttons.forEach((button_index,button) -> {
			if(co[button_index].getPollData() > 0.5f && button.old_state == RELEASED) {
				button.listener.execute(PRESSED);
				button.old_state = PRESSED;
			}
			if(co[button_index].getPollData() < 0.5f && button.old_state == PRESSED) {
				button.listener.execute(RELEASED);
				button.old_state = RELEASED;
			}
		});
	}

	public void scanControls(int t, int y, int p, int r) {
		if(Math.abs(t-throttle)>THRESHOLD   || Math.abs(y-yaw)>THRESHOLD ||
				Math.abs(p-pitch)>THRESHOLD || Math.abs(r-roll)>THRESHOLD ||
				(System.currentTimeMillis() - tms)>TIMEOUT) {
			controls.forEach((listener) -> {
				listener.execute(t, y, p, r);
			});

			this.throttle = t;
			this.yaw      = y;
			this.pitch    = p;
			this.roll     = r;
			this.tms      = System.currentTimeMillis();
		}
	}


	private class button {

		public int                  old_state = RELEASED;
		public IButtonListener     listener = null;

		button(IButtonListener listener) {
			this.listener = listener;
		}
	}


}
