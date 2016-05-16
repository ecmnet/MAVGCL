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

package com.comino.flight.control;

import org.mavlink.messages.lquac.msg_manual_control;

import com.comino.mav.control.IMAVController;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class SITLController implements Runnable {


	private static final int 	ROLL     = 25;
	private static final int 	PITCH    = 26;
	private static final int 	YAW      = 27;
	private static final int 	THROTTLE = 24;

	private Controller        pad		   = null;

	private IMAVController control;
	private msg_manual_control rc = new msg_manual_control(255,1);
	private Component[] components;

	public SITLController(IMAVController control) {

		this.control = control;

		Controller[] ca = null;
		try {
			ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
		} catch( java.lang.UnsatisfiedLinkError u) {
			u.printStackTrace();
			return;
		}

		for(int i=0;i<ca.length && pad==null;i++) {
			if(ca[i].getType()==Controller.Type.GAMEPAD)
				pad = ca[i];
		}

		if(pad==null)
			return;

		System.out.println(pad.getName()+" connected");

		this.components = pad.getComponents();

		new Thread(this).start();
	}


	@Override
	public void run() {
		while(true) {
			try {
				pad.poll();

				rc.z = (int)(components[THROTTLE].getPollData()*1000);
				rc.r = (int)(components[YAW].getPollData()*1000);
				rc.x = (int)(components[PITCH].getPollData()*1000);
				rc.y = (int)(components[ROLL].getPollData()*1000);

				if(control.isConnected())
				  control.sendMAVLinkMessage(rc);

				Thread.sleep(20);

			} catch(Exception e ) {  }
		}
	}
}
