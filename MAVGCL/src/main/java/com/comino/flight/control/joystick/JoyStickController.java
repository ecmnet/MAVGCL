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
import org.mavlink.messages.MSP_AUTOCONTROL_ACTION;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.observables.StateProperties;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.mavlink.MAV_CUST_MODE;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.Status;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;



public class JoyStickController implements Runnable {


	private ControllerManager  pad		  = null;
	private IMAVController    control     = null;
	private ControllerState   components  = null;


	// Switches
	private int ch_land     = 0; //cross
	private int ch_arm      = 1; //circle
	private int ch_takeoff  = 3; //triangle
	private int ch_kill     = 4; //options
	private int ch_rtl      = 2; //square
	private int ch_seq      = 5; //DpDown



	private JoyStickModel joystick = new JoyStickModel();

	private DataModel model;

	@SafeVarargs
	public JoyStickController(IMAVController control, Class<?> ...adapters) {
		this.control = control;

		if(control!=null)
		  this.model   = control.getCurrentModel();


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
				vibrate();
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

				msg_msp_command msp = new msg_msp_command(255,1);
				msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
				msp.param2 =  MSP_AUTOCONTROL_ACTION.RTL;

				if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_ACTION.RTL))
					msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
				else
					msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
				control.sendMAVLinkMessage(msp);

			}
		});

		joystick.addButtonListener(ch_seq, (state) -> {
			if(state == JoyStickModel.PRESSED) {
                // Execute sequence 1
				msg_msp_command msp = new msg_msp_command(255,1);
				msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
				msp.param2 =  MSP_AUTOCONTROL_ACTION.TEST_SEQ1;
				control.sendMAVLinkMessage(msp);

			}
		});


		joystick.addControlListener((t,y,p,r) -> {
			// System.out.println("Throttle="+deadzone((t-1500f)/-1000f,0.05f)+" Yaw="+deadzone((y-1500f)/-1000f,0.05f)+" Pitch="+deadzone((p-1500f)/ 1000f,0.02f)+" Roll="+deadzone((r-1500f)/-1000f,0.02f));

			if(control.isConnected())
			  control.sendMSPLinkCmd(MSP_CMD.MSP_CMD_OFFBOARD_SETLOCALVEL, MSP_COMPONENT_CTRL.ENABLE,
					deadzone((p-1500.0f)/ 1000.0f,0.02f),
					deadzone((r-1500.0f)/-1000.0f,0.02f),
					deadzone((t-1500.0f)/-1000.0f,0.10f),
					deadzone((y-1500.0f)/-1000.0f,0.05f));
		});
		
		if(isConnected()) {
		Thread thread = new Thread(this);
		thread.setName("Joystick worker");
		thread.start();
		}


	}

	public void vibrate() {
		pad.doVibration(0, 0.2f,0.2f, 20);
	}


	public boolean isConnected() {
		if(this.components==null)
			return false;
		return components.isConnected;
	}

	private float deadzone(float val, float dz ) {
		if( Math.abs(val) < dz )
			return 0;
		return val;
	}



	@Override
	public void run() {

		this.pad = new ControllerManager();
		this.pad.initSDLGamepad();

		while(true) {
			try {

				components = pad.getState(0);

				// TODO: Disconnecting while running is not detected; Reconnect in this case
				if(!components.isConnected ) {
					StateProperties.getInstance().getControllerConnectedProperty().set(false);
					Thread.sleep(1000);
					pad.update();
					continue;
				}

				StateProperties.getInstance().getControllerConnectedProperty().set(true);

				joystick.scanControls(components);
				joystick.scanButtons(components);

				Thread.sleep(50);

			} catch(Exception e ) {
				e.printStackTrace();
				control.getCurrentModel().sys.setStatus(Status.MSP_RC_ATTACHED, false);
			}
		}
	}

}
