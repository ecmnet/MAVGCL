/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;

import com.comino.flight.observables.StateProperties;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.mavlink.MAV_CUST_MODE;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.Status;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class JoyStickController implements Runnable {


	private Controller     pad		   = null;
	private IMAVController control     = null;
	private Component[]    components  = null;

	// Controls
	private int ch_throttle=0;
	private int ch_yaw=0;
	private int ch_pitch=0;
	private int ch_roll=0;

	// Switches
	private int ch_land     = 0;
	private int ch_arm      = 0;
	private int ch_takeoff  = 0;
	private int ch_kill     = 0;
	private int ch_rtl      = 0;
	private int ch_sw6      = 0;

	// Th
	private int ch_sign= 1;


	private JoyStickModel joystick = new JoyStickModel();
	private Class<?>[]    adapters;

	private DataModel model;

	@SafeVarargs
	public JoyStickController(IMAVController control, Class<?> ...adapters) {
		this.adapters = adapters;
		this.control = control;

		if(control!=null)
		  this.model   = control.getCurrentModel();
	}

	public boolean connect() {

		System.out.println("Searching for controllers..");

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

		if(pad==null) {
			System.out.println("No controllers connected");
			return false;
		}

		this.components = pad.getComponents();


		try {

			boolean found = false;
			for(Class<?> adapter : adapters) {

				if(pad.getName().contains((String) adapter.getField("NAME").get(null))) {
					this.ch_throttle   = adapter.getField("THROTTLE").getInt(null);
					this.ch_yaw        = adapter.getField("YAW").getInt(null);
					this.ch_pitch      = adapter.getField("PITCH").getInt(null);
					this.ch_roll       = adapter.getField("ROLL").getInt(null);
					this.ch_sign       = adapter.getField("SIGN").getInt(null);
					this.ch_land       = adapter.getField("LAND").getInt(null);
					this.ch_arm        = adapter.getField("ARM").getInt(null);
					this.ch_takeoff    = adapter.getField("TAKEOFF").getInt(null);
					this.ch_kill       = adapter.getField("KILL").getInt(null);
					this.ch_rtl        = adapter.getField("RTL").getInt(null);
					this.ch_sw6        = adapter.getField("SW6").getInt(null);
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


		joystick.addButtonListener(ch_land, (state) -> {
			if(state == JoyStickModel.PRESSED)
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_NAV_LAND, 0, 2, 0.05f );
		});

		joystick.addButtonListener(ch_arm, (state) -> {
			if(state == JoyStickModel.PRESSED) {

				if(!model.sys.isStatus(Status.MSP_ARMED)) {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,1 );
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
							MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
							MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_MANUAL, 0 );
				} else {
					if(model.sys.isStatus(Status.MSP_LANDED))
						control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,0 );
				}

			}
		});

		joystick.addButtonListener(ch_takeoff, (state) -> {
			if(state == JoyStickModel.PRESSED && model.sys.isStatus(Status.MSP_LANDED)) {

				if(model.hud.ag!=Float.NaN && model.sys.isStatus(Status.MSP_LPOS_VALID) ) {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_NAV_TAKEOFF, -1, 0, 0, Float.NaN, Float.NaN, Float.NaN,
							model.hud.at);
				}
				else {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,0 );
					MSPLogger.getInstance().writeLocalMsg("[mgc] Takoff rejected: LPOS not available",
							MAV_SEVERITY.MAV_SEVERITY_WARNING);
				}
			}
		});

		joystick.addButtonListener(ch_kill, (state) -> {
			if(state == JoyStickModel.PRESSED) {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM, (cmd,result) -> {
					if(result==0) {
						MSPLogger.getInstance().writeLocalMsg("EMERGENCY: User requested to switch off motors",
								MAV_SEVERITY.MAV_SEVERITY_EMERGENCY);
					}
				},0, 21196 );
			}
		});

		joystick.addButtonListener(ch_rtl, (state) -> {
			if(state == JoyStickModel.PRESSED) {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_AUTO, MAV_CUST_MODE.PX4_CUSTOM_SUB_MODE_AUTO_RTL);
			}
		});

		joystick.addControlListener((t,y,p,r) -> {
			// System.out.println("Throttle="+deadzone((t-1500f)/-1000f,0.05f)+" Yaw="+deadzone((y-1500f)/-1000f,0.05f)+" Pitch="+deadzone((p-1500f)/ 1000f,0.02f)+" Roll="+deadzone((r-1500f)/-1000f,0.02f));

			if(control.isConnected())
			  control.sendMSPLinkCmd(MSP_CMD.MSP_CMD_OFFBOARD_SETLOCALVEL, MSP_COMPONENT_CTRL.ENABLE,
					deadzone((p-1500.0f)/ 1000.0f,0.02f),
					deadzone((r-1500.0f)/-1000.0f,0.02f),
					deadzone((t-1500.0f)/-1000.0f,0.05f),
					deadzone((y-1500.0f)/-1000.0f,0.05f));
		});

		Thread t = new Thread(this);
		t.setName("Joystick worker");
		t.start();

		return true;
	}

	private float deadzone(float val, float dz ) {
		if( Math.abs(val) < dz )
			return 0;
		return val;
	}



	@Override
	public void run() {

		//		while(!control.isConnected()) {
		//			try {
		//				Thread.sleep(500);
		//			} catch (InterruptedException e) { }
		//		}

	//	StateProperties.getInstance().getControllerConnectedProperty().set(true);


		while(pad.poll()) {
			try {


				joystick.scanControls((int)(components[ch_throttle].getPollData()*500*ch_sign+1500),
						(int)(components[ch_yaw].getPollData()  *500*ch_sign+1500),
						(int)(components[ch_pitch].getPollData()*500*ch_sign+1500),
						(int)(components[ch_roll].getPollData() *500*ch_sign+1500) );

				joystick.scanButtons(components);



				//				for(int i =0; i < components.length; i++)
				//					System.out.print(i+":"+components[i].getIdentifier().getName()+": "+components[i].getPollData());
				//				System.out.println();


				Thread.sleep(10);

			} catch(Exception e ) {
				e.printStackTrace();
				control.getCurrentModel().sys.setStatus(Status.MSP_RC_ATTACHED, false);
			}
		}
		StateProperties.getInstance().getControllerConnectedProperty().set(false);
		System.out.println("Disconnected");
	}

}
