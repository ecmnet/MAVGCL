package com.comino.flight.control.joystick;

import java.util.ArrayList;
import java.util.HashMap;

import com.studiohartman.jamepad.ControllerState;


public class JoyStickModel {

	public static  int   PRESSED   = 1;
	public static  int   RELEASED  = 0;

	private static float  THRESHOLD  = 0.05f;
	private static int    TIMEOUT    = 500;

	public float throttle = 0;
	public float yaw      = 0;
	public float pitch    = 0;
	public float roll     = 0;

	private long    tms    = 0;
	private boolean button_state = false;


	private HashMap<Integer,button>     buttons  = new HashMap<Integer,button>();
	private ArrayList<IControlListener> controls = new ArrayList<IControlListener>();

	public void addButtonListener(int button_index, IButtonListener listener ) {
		buttons.put(button_index,new button(listener));
	}

	public void addControlListener(IControlListener listener) {
		controls.add(listener);
	}


	public void scanButtons(ControllerState state) {

		buttons.forEach((button_index,button) -> {

			switch(button_index) {
			case 0:
				button_state = state.a;
				break;
			case 1:
				button_state = state.b;
				break;
			case 2:
				button_state = state.x;
				break;
			case 3:
				button_state = state.y;
				break;
			case 4:
				button_state = state.start;
				break;
			case 5:
				button_state = state.dpadDown;
				break;
			default:
				return;
			}

			if(button_state && button.old_state == RELEASED) {
				button.listener.execute(PRESSED);
				button.old_state = PRESSED;
			}

			if(!button_state && button.old_state == PRESSED) {
				button.listener.execute(RELEASED);
				button.old_state = RELEASED;
			}


		});
	}

	public void scanControls(ControllerState state) {

		if(Math.abs(state.leftStickY-throttle)>THRESHOLD   ||
		   Math.abs(state.leftStickX-yaw)>THRESHOLD ||
		   Math.abs(state.rightStickY-pitch)>THRESHOLD ||
		   Math.abs(state.rightStickX-roll)>THRESHOLD ||
		   (System.currentTimeMillis() - tms)>TIMEOUT) {
			controls.forEach((listener) -> {
				listener.execute((int)(state.leftStickY*  500f+1500f),
						         (int)(state.leftStickX* -500f+1500f),
						         (int)(state.rightStickY* 500f+1500f),
						         (int)(state.rightStickX*-500f+1500f));
			});

			this.throttle = state.leftStickY;
			this.yaw      = state.leftStickX;
			this.pitch    = state.rightStickY;
			this.roll     = state.rightStickX;
			this.tms      = System.currentTimeMillis();
		}
	}


	private class button {

		public int                 old_state = RELEASED;
		public IButtonListener     listener = null;

		button(IButtonListener listener) {
			this.listener = listener;
		}
	}


}
