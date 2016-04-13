package com.comino.flight.control;

import java.util.prefs.Preferences;

public class MAVPreferences {

	private static Preferences prefs = null;

	public static Preferences getInstance() {
		if(prefs==null)
			prefs = Preferences.userRoot().node("com.comino.mavgcl");
		return prefs;
	}

}
