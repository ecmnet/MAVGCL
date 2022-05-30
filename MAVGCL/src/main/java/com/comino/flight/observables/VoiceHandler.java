package com.comino.flight.observables;

/****************************************************************************
*
*   Copyright (c) 2022 Eike Mansfeld ecm@gmx.de. All rights reserved.
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


import org.mavlink.messages.MAV_SEVERITY;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavutils.workqueue.WorkQueue;
import com.comino.speech.VoiceTTS;

public class VoiceHandler {

	private static VoiceHandler instance;

	private final WorkQueue         wq = WorkQueue.getInstance();
	private final AnalysisDataModel model;
	private final StateProperties   properties;
	private final VoiceTTS          voice;

	public static VoiceHandler getInstance(IMAVController control) {
		if(instance==null)
			instance = new VoiceHandler(control);
		return instance;
	}

	private VoiceHandler(IMAVController control) {

		this.model      = AnalysisModelService.getInstance().getCurrent();
		this.properties = StateProperties.getInstance();
		this.voice      = VoiceTTS.getInstance();

		if(!MAVPreferences.getInstance().getBoolean("SPEECH", false))
			return;

		control.addMAVMessageListener(msg -> {
			
			if(!properties.getArmedProperty().get())
				return;
			
			if(msg.severity != MAV_SEVERITY.MAV_SEVERITY_EMERGENCY && msg.severity != MAV_SEVERITY.MAV_SEVERITY_CRITICAL)
				return;
			if(msg.text.contains("]"))
				voice.talk(msg.text.substring(msg.text.indexOf(']')));
		});

		// Report takeoff and landed state if armed
		properties.getLandedProperty().addListener((s,o,n) -> {

			if(!properties.getArmedProperty().get())
				return;

			if(o.booleanValue() && !n.booleanValue()) {
				voice.talk("Takeoff");
			}

			if(!o.booleanValue() && n.booleanValue()) {
				voice.talk("Landed");
			}
		});


		// report battery status every 30 seconds if armed and below 60%
		wq.addCyclicTask("LP", 30000, () -> {
			if(!properties.getArmedProperty().get())
				return;
			int v = (int)(model.getValue("BATP")*10f);			
			switch(v) {
			case 0:
				voice.talk("Battery is below 10 percent."); break;
			case 1:
				voice.talk("Battery is below 20 percent."); break;
			case 2:
				voice.talk("Battery is below 30 percent."); break;
			case 3:
				voice.talk("Battery is below 40 percent."); break;
			case 4:
				voice.talk("Battery is below 50 percent."); break;
			}
				
		});

		// report altitude every 50 seconds
		wq.addCyclicTask("LP", 50000, () -> {
			if(!properties.getArmedProperty().get() || properties.getLandedProperty().get())
				return;
			voice.talk(String.format("Relative altitude is %.1f meters.",model.getValue("ALTRE")));
		});



	}

}
