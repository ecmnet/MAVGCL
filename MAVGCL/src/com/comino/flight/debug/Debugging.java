package com.comino.flight.debug;

import com.comino.mav.control.IMAVController;

public class Debugging {

	public static void registerDebugging(IMAVController control) {

		/*  EXAMPLE for Debugging values:

		control.getCollector().addDebugListener(model -> {
			model.debug.x = (float) Math.sin(model.attitude.h);
		});

		*/

	}

}
