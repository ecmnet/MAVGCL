package com.comino.flight.parameter;

import java.util.ArrayList;
import java.util.List;

import org.mavlink.messages.lquac.msg_param_request_list;
import org.mavlink.messages.lquac.msg_param_value;

import com.comino.flight.observables.StateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class PX4Parameters implements IMAVLinkListener {

	private static PX4Parameters px4params = null;

	private BooleanProperty isReadyProperty = new SimpleBooleanProperty();

	private List<ParameterAttributes> parameterList = null;

	private IMAVController control;

	private ParameterFactMetaData metadata = null;

	private int totalCount = 0;

	public static PX4Parameters getInstance(IMAVController control) {
		if(px4params==null)
			px4params = new PX4Parameters(control);
		return px4params;
	}

	public static PX4Parameters getInstance() {
		return px4params;
	}


	private PX4Parameters(IMAVController control) {
		this.control  = control;
		this.control.addMAVLinkListener(this);

		this.metadata = new ParameterFactMetaData("PX4ParameterFactMetaData.xml");
		this.parameterList = new ArrayList<ParameterAttributes>();

		StateProperties.getInstance().getConnectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
				refreshParameterList();
			}
		});
	}

	public void refreshParameterList() {
		parameterList.clear();
		isReadyProperty().setValue(false);
		msg_param_request_list msg = new msg_param_request_list(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
	}


	public BooleanProperty isReadyProperty() {
		return isReadyProperty;
	}

	@Override
	public void received(Object _msg) {

		if( _msg instanceof msg_param_value) {
			msg_param_value msg = (msg_param_value)_msg;
			this.totalCount = msg.param_count;

			if(msg.param_id[0]=='_')
				return;

			ParameterAttributes attributes = metadata.getMetaData(msg.getParam_id());
			if(attributes == null)
				attributes = new ParameterAttributes(msg.getParam_id(),"(DefaultGroup)");
			attributes.value = msg.param_value;
			attributes.vtype = msg.param_type;
			parameterList.add(attributes);
			if(parameterList.size() == totalCount)
				isReadyProperty().setValue(true);
		}
	}

	public ParameterFactMetaData getMetaData() {
		return this.metadata;
	}

	public List<ParameterAttributes> getList() {
		return this.parameterList;
	}

}
