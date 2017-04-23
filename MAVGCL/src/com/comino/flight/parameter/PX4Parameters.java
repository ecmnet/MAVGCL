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

package com.comino.flight.parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_param_request_list;
import org.mavlink.messages.lquac.msg_param_value;

import com.comino.flight.observables.StateProperties;
import com.comino.jfx.extensions.Badge;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class PX4Parameters implements IMAVLinkListener {

	private static PX4Parameters px4params = null;

	private ObjectProperty<ParameterAttributes> property = new SimpleObjectProperty<ParameterAttributes>();

	private Map<String,ParameterAttributes> parameterList = null;

	private IMAVController control;

	private ParameterFactMetaData metadata = null;

	private StateProperties stateProperties =  null;

	private List<IPX4ParameterRefresh> refreshListeners = new ArrayList<IPX4ParameterRefresh>();

	private boolean is_reading = false;

	private ScheduledFuture timeout = null;

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
		this.stateProperties = StateProperties.getInstance();

		this.metadata = new ParameterFactMetaData("PX4ParameterFactMetaData.xml");
		this.parameterList = new HashMap<String,ParameterAttributes>();

		StateProperties.getInstance().getConnectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
				if(newValue && control.isConnected()) {
					refreshParameterList(true);
				}
			}
		});


		StateProperties.getInstance().getLogLoadedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
				if(!newValue && control.isConnected())
					refreshParameterList(true);
			}
		});

		StateProperties.getInstance().getConnectedProperty().addListener((e,o,n) -> {
			if(!n.booleanValue()) {
				is_reading = false;
				stateProperties.getProgressProperty().set(StateProperties.NO_PROGRESS);
				parameterList.clear();
			}
		});

	}

	public void refreshParameterList(boolean loaded) {
		if(!is_reading) {
			property.setValue(null);
			parameterList.clear();
			msg_param_request_list msg = new msg_param_request_list(255,1);
			msg.target_component = 1;
			msg.target_system = 1;
			control.sendMAVLinkMessage(msg);
			stateProperties.getParamLoadedProperty().set(!loaded);
			MSPLogger.getInstance().writeLocalMsg("Reading parameters...",
					MAV_SEVERITY.MAV_SEVERITY_INFO);
			is_reading = true;
			timeout = ExecutorService.get().schedule(() -> {
				stateProperties.getParamLoadedProperty().set(false);
				stateProperties.getProgressProperty().set(StateProperties.NO_PROGRESS);
				MSPLogger.getInstance().writeLocalMsg("Timeout reading parameters",
						MAV_SEVERITY.MAV_SEVERITY_WARNING);
				is_reading = false;
			}, 20, TimeUnit.SECONDS);
		}
	}


	public ObjectProperty<ParameterAttributes> getAttributeProperty() {
		return property;
	}

	@Override
	public void received(Object _msg) {

		long flight_time = 0;

		if( _msg instanceof msg_param_value) {

			property.setValue(null);

			msg_param_value msg = (msg_param_value)_msg;

			if(msg.param_id[0]=='_')
				return;

			ParameterAttributes attributes = metadata.getMetaData(msg.getParam_id());
			if(attributes == null)
				attributes = new ParameterAttributes(msg.getParam_id(),"(DefaultGroup)");
			attributes.value = ParamUtils.paramToVal(msg.param_type, msg.param_value);
			attributes.vtype = msg.param_type;


			parameterList.put(attributes.name,attributes);
			property.setValue(attributes);

			if(is_reading)
			  stateProperties.getProgressProperty().set((float)msg.param_index/msg.param_count);

			if(msg.param_index >= msg.param_count-1) {
				timeout.cancel(true);
				stateProperties.getParamLoadedProperty().set(true);
				stateProperties.getProgressProperty().set(StateProperties.NO_PROGRESS);
				for(IPX4ParameterRefresh l : refreshListeners)
					l.refresh();
				is_reading = false;
				if(get("LND_FLIGHT_T_LO")!=null) {
					flight_time = (((long)get("LND_FLIGHT_T_HI").value << 32 ) + (long)get("LND_FLIGHT_T_LO").value);
					if(flight_time <1e10f && flight_time > 0)
						MSPLogger.getInstance().writeLocalMsg(String.format("Total flight time: %5.2f min", flight_time/60e6f),
								MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				}
			}
		}
	}

	public void setParametersFromLog(Map<String,Object> list) {
		StateProperties.getInstance().getParamLoadedProperty().set(false);
		parameterList.clear();
		list.forEach((s,o) -> {
			ParameterAttributes attributes = metadata.getMetaData(s);
			if(attributes == null)
				attributes = new ParameterAttributes(s,"(DefaultGroup)");

			if(o instanceof Float)
				attributes.value = ((Float)(o)).floatValue();
			if(o instanceof Integer)
				attributes.value = ((Integer)(o)).floatValue();

			parameterList.put(attributes.name,attributes);
			property.setValue(attributes);
		});
		stateProperties.getParamLoadedProperty().set(true);
		for(IPX4ParameterRefresh l : refreshListeners)
			l.refresh();
	}

	public void addRefreshListener(IPX4ParameterRefresh listener) {
		refreshListeners.add(listener);
	}


	public ParameterFactMetaData getMetaData() {
		return this.metadata;
	}

	public List<ParameterAttributes> getList() {
		return asSortedList(this.parameterList);
	}

	public ParameterAttributes get(String name) {
		return parameterList.get(name);
	}

	private List<ParameterAttributes> asSortedList(Map<String,ParameterAttributes> c) {
		List<ParameterAttributes> list = new ArrayList<ParameterAttributes>(c.values());
		java.util.Collections.sort(list);
		return list;
	}

}
