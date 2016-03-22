package com.comino.flight.control.integration;

import com.comino.mav.control.IMAVController;

public class AnalysisIntegration {

	public static void registerFunction(IMAVController control) {

		/*  EXAMPLE for Debugging values */

		control.getCollector().addInjectionListener(model -> {
			model.debug.x = (float) Math.asin(model.attitude.aY);
		});

	}

}
