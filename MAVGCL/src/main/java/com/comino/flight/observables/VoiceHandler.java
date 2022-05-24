package com.comino.flight.observables;

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
	
	private static final float BATTERY_CAPACITY_LIMT = 60.0f;

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
			float v = (float)model.getValue("BATP")*100f;
			if(v < BATTERY_CAPACITY_LIMT)
				voice.talk(String.format("Battery is at %.0f percent.",v));
		});

		// report altitude every 45 seconds
		wq.addCyclicTask("LP", 45000, () -> {
			if(!properties.getArmedProperty().get() || properties.getLandedProperty().get())
				return;
			voice.talk(String.format("Relative altitude is %.1f meters.",model.getValue("ALTRE")));
		});



	}

}
