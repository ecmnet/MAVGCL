package com.comino.flight.control.joystick;

public interface IControlListener {

	public void execute(int throttle, int yaw, int pitch, int roll);

}