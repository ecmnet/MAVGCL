/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.experimental;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_FRAME;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.lquac.msg_set_position_target_local_ned;
import org.mavlink.messages.lquac.msg_vision_position_estimate;

import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;
import com.comino.msp.model.segment.Status;

public class OffboardSimulationUpdater implements Runnable {

	private IMAVController control = null;
	private boolean isRunning = false;

	private float altitude = -1.5f;
	private float x_pos = 0f;
	private float y_pos = 0f;

	public OffboardSimulationUpdater(IMAVController control) {
		this.control = control;
		this.control.addModeChangeListener((oldstatus, newstatus) -> {
			if(!newstatus.isStatus(Status.MSP_MODE_OFFBOARD))
				isRunning = false;
		});
	}

	public void start() {

		if(!control.isSimulation()) {
			System.out.println("OFFBOARD only for SITL !");
			return;
		}
		isRunning = true;

		new Thread(this).start();

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) { }

		if(!control.getCurrentModel().sys.isStatus(Status.MSP_MODE_OFFBOARD))
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
					MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
					MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_OFFBOARD, 0 );

	}

	public boolean isRunning() {
		return isRunning;
	}

	public void stop() {
		isRunning = false;
	}

	public void setAltitude(float altitude) {
		this.altitude = altitude;
	}

	public void setX(float x) {
		this.x_pos = x;;
	}

	public void setY(float y) {
		this.y_pos = y;;
	}


	@Override
	public void run() {


		if(!isRunning)
			return;


		System.out.println("Offboard updater started");

		while(isRunning) {


			msg_set_position_target_local_ned cmd = new msg_set_position_target_local_ned(255,1);
			cmd.target_component = 1;
			cmd.target_system = 1;
			cmd.type_mask = 0b000111111111000;
			cmd.x =  x_pos;
			cmd.y =  y_pos;
			cmd.z =  altitude;
			cmd.coordinate_frame = MAV_FRAME.MAV_FRAME_LOCAL_NED;


			if(!control.sendMAVLinkMessage(cmd))
				stop();

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { }

//			if(!control.getCurrentModel().sys.isStatus(Status.MSP_MODE_OFFBOARD))
//				stop();
		}

		System.out.println("Offboard updater stopped");

		if(control.getCurrentModel().sys.isStatus(Status.MSP_MODE_OFFBOARD))
		control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
				MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
				MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_POSCTL, 0 );



	}

}
