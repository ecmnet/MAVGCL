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

import org.mavlink.messages.lquac.msg_vision_position_estimate;
import org.mavlink.messages.lquac.msg_vision_speed_estimate;

import com.comino.mav.control.IMAVController;

public class VisionSpeedSimulationUpdater implements Runnable {

	private IMAVController control = null;
	private boolean isRunning = false;

	private float x=0;
	private float y=0;

	public VisionSpeedSimulationUpdater(IMAVController control) {
		this.control = control;
	}

	public void start() {
		System.out.println("Vision updater started");
		isRunning = true;
		new Thread(this).start();

	}

	public void setSpeedX(float x) {
		this.x=x;
	}

	public void setSpeedY(float y) {
		this.y=y;
	}

	public void stop() {
		System.out.println("Vision updater stopped");
        isRunning = false;
	}


	@Override
	public void run() {

		while(isRunning) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { }

			msg_vision_speed_estimate cmd = new msg_vision_speed_estimate(1,1);
			cmd.x = x;
			cmd.y = y;
			cmd.z = 0;
			System.out.println(x+":"+y);
			if(!control.sendMAVLinkMessage(cmd))
				stop();


		}



	}

}
