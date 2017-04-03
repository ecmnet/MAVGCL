/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.prefs;

import java.util.prefs.Preferences;


public class MAVPreferences {

	public final static String PREFS_IP_ADDRESS = "IP_ADDRESS";
	public final static String PREFS_IP_PORT    = "IP_PORT";
	public final static String PREFS_BIND_PORT  = "BIND_PORT";
	public final static String PREFS_SITL       = "SITL";
	public final static String PREFS_VIDEO      = "VIDEOURL";
	public final static String PREFS_DIR        = "DIRECTORY";
	public final static String AUTOSAVE         = "AUTOSAVE";
	public final static String PREFS_XMLDIR     = "XMLDIRECTORY";
	public final static String RECENT_FIGS      = "RECENTFIGS";
	public final static String LINECHART_FIG_1  = "LINECHARTFIG1";
	public final static String LINECHART_FIG_2  = "LINECHARTFIG2";
	public final static String LINECHART_FIG_3  = "LINECHARTFIG3";
	public final static String XYCHART_FIG_1    = "XYCHARTFIG1";
	public final static String XYCHART_FIG_2    = "XYCHARTFIG2";
	public final static String XYCHART_SCALE    = "XYCHARTSCALE";
	public final static String XYCHART_CENTER   = "XYCHARTCENTER";
	public final static String XYCHART_OFFSET   = "XYCHARTOFFSET";
	public final static String XYCHART_SLAM     = "XYCHARTSLAM";
	public final static String ULOGGER          = "ULOGGER";
	public final static String TUNING_GROUP     = "TUNING_GROUP";
	public final static String CTRLPOS          = "CTRLPOS";
	public final static String HEALTHCHECK      = "HEALTHCHECK";
	public final static String RTKSVINACC       = "RTKSVINACC";

	private static Preferences prefs = null;


	public static Preferences getInstance() {
		if(prefs==null) {
			prefs = Preferences.userRoot().node("com.comino.mavgcl");
		}
		return prefs;
	}

}
