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

package com.comino.flight.control.joystick;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_manual_control;

import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;
import com.comino.msp.log.MSPLogger;

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
					System.out.println("[mgc]"+pad.getName()
					               +" connected to adapter "+adapter.getSimpleName());
					break;
				}
			}
			if(!found)
				throw new Exception("Controller "+pad.getName()+" not registered");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(this.getClass().getSimpleName()+":"+e.getMessage());
			return false;
		}

		Thread t = new Thread(this);
		t.setName("Joystick worker");
		t.start();

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

				if(state_sw2 != (int)components[ch_sw2].getPollData() ) {
					state_sw2 = (int)components[ch_sw2].getPollData();
					if(components[ch_sw1].getPollData() > -0.5 ) {
						if((int)components[ch_sw2].getPollData() != 0) {
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
