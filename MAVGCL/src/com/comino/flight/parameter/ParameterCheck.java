package com.comino.flight.parameter;

import org.mavlink.messages.lquac.msg_param_value;

import com.comino.msp.main.control.listener.IMAVLinkListener;

public class ParameterCheck implements IMAVLinkListener {


	public ParameterCheck() {

	}


	@Override
	public void received(Object _msg) {

		if( _msg instanceof msg_param_value)
			checkParameter((msg_param_value)_msg);
	}

	private void checkParameter(msg_param_value msg) {

		if(msg.param_id[0]=='_')
			return;


	}
}
