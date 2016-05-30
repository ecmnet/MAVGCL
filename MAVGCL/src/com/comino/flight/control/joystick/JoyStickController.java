package com.comino.flight.control.joystick;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.lquac.msg_manual_control;

import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class JoyStickController implements Runnable {

	private Controller     pad		   = null;
	private IMAVController control     = null;
	private Component[]    components  = null;

	private int ch_throttle=0;
	private int ch_yaw=0;
	private int ch_pitch=0;
	private int ch_roll=0;
	private int ch_sw1 = 0;
	private int ch_sw2 = 0;

	private int state_sw2 = -1;

	private int ch_sign= 1;

	private msg_manual_control rc = new msg_manual_control(255,1);
	private Class<?>[] adapters;

	@SafeVarargs
	public JoyStickController(IMAVController control, Class<?> ...adapters) {
		this.adapters = adapters;
		this.control = control;
	}

	public boolean connect() {

		Controller[] ca = null;
		try {
			ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
		} catch( java.lang.UnsatisfiedLinkError u) {
			u.printStackTrace();
			return false;
		}

		for(int i=0;i<ca.length && pad==null;i++) {
			if(ca[i].getType()==Controller.Type.GAMEPAD)
				pad = ca[i];
		}

		if(pad==null)
			return false;

		this.components = pad.getComponents();


		try {

			boolean found = false;
			for(Class<?> adapter : adapters) {
				if(pad.getName().contains((String) adapter.getField("NAME").get(null))) {
					this.ch_throttle = adapter.getField("THROTTLE").getInt(null);
					this.ch_yaw = adapter.getField("YAW").getInt(null);
					this.ch_pitch = adapter.getField("PITCH").getInt(null);
					this.ch_roll = adapter.getField("ROLL").getInt(null);
					this.ch_sign = adapter.getField("SIGN").getInt(null);
					this.ch_sw1 = adapter.getField("SW1").getInt(null);
					this.ch_sw2 = adapter.getField("SW2").getInt(null);
					found = true;
					System.out.println(pad.getName()+" connected to adapter "+adapter.getSimpleName());
					break;
				}
			}
			if(!found)
				throw new Exception("Controller "+pad.getName()+" not registered");
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return false;
		}

		new Thread(this).start();

		return true;
	}



	@Override
	public void run() {

		while(!control.isConnected()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}

		while(true) {
			try {
				pad.poll();

				rc.z = (int)(components[ch_throttle].getPollData()*500*ch_sign+500);
				rc.r = (int)(components[ch_yaw].getPollData()*1000*ch_sign);
				rc.x = (int)(components[ch_pitch].getPollData()*1000*ch_sign);
				rc.y = (int)(components[ch_roll].getPollData()*1000*ch_sign);

				if(rc.z<20 && rc.r > 980) {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,1 );
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
							MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
							MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_POSCTL, 0 );
				}

				if(rc.z<20 && rc.r < -980) {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,0 );
				}



				if(control.isConnected())
					control.sendMAVLinkMessage(rc);

				// Simple switch mapping for ALT/POS-CTL

				if(state_sw2 != components[ch_sw2].getPollData() ) {
					if(components[ch_sw1].getPollData() == 0 ) {
						if((int)components[ch_sw2].getPollData()==0) {
							control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
									MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
									MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_ALTCTL, 0 );
						} else {
							control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
									MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
									MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_POSCTL, 0 );
						}
					} else {
						control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
								MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
								MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_MANUAL, 0 );
					}
				}


				//								 for(int i =14; i < components.length; i++)
				//								    System.out.print(i+":"+components[i].getIdentifier().getName()+": "+components[i].getPollData());
				//								 System.out.println();


				Thread.sleep(50);

			} catch(Exception e ) {  }
		}
	}

}
