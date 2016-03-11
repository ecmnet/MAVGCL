package com.comino.flight.log;

import com.comino.mav.control.IMAVController;

public class MAVGCLLog {

	private static MAVGCLLog log = null;
	private IMAVController control = null;
	private boolean debug_msg_enabled = false;


	public static MAVGCLLog getInstance(IMAVController control) {
		if(log==null) {
			log = new MAVGCLLog(control);
		}
		return log;
	}

	public static MAVGCLLog getInstance() {
		return log;
	}

	private MAVGCLLog(IMAVController control2) {
		this.control = control2;
	}

	public void enableDebugMessages(boolean enabled) {
		this.debug_msg_enabled = enabled;
	}

	public void writeLocalMsg(String msg) {
		control.writeMessage("GCL: "+msg);
	}

	public void writeLocalDebugMsg(String msg) {
		if(debug_msg_enabled)
		   control.writeMessage("DBG: "+msg);
	}

}
