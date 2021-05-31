/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.param;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_param_request_list;
import org.mavlink.messages.lquac.msg_param_value;

import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.weather.MetarQNHService;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.mavlink.IMAVLinkListener;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.param.IPX4ParameterRefresh;
import com.comino.mavcom.param.PX4Parameters;
import com.comino.mavcom.param.ParamUtils;
import com.comino.mavcom.param.ParameterAttributes;
import com.comino.mavcom.param.ParameterFactMetaData;
import com.comino.mavutils.workqueue.WorkQueue;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class MAVGCLPX4Parameters extends PX4Parameters implements IMAVLinkListener {

	private static MAVGCLPX4Parameters px4params = null;

	private ObjectProperty<ParameterAttributes> property = new SimpleObjectProperty<ParameterAttributes>();

	private Preferences preferences = null;

	private StateProperties state =  null;

	private List<IPX4ParameterRefresh> refreshListeners = new ArrayList<IPX4ParameterRefresh>();

	private boolean is_reading   = false;

	//	private ScheduledFuture<?> timeout = null;
	private int timeout=0;
	private float qnh;

	private final WorkQueue wq = WorkQueue.getInstance();

	public static MAVGCLPX4Parameters getInstance(IMAVController control) {
		if(px4params==null)
			px4params = new MAVGCLPX4Parameters(control);
		return px4params;
	}

	public static MAVGCLPX4Parameters getInstance() {
		return px4params;
	}


	private MAVGCLPX4Parameters(IMAVController control) {
		super(control);

		this.state = StateProperties.getInstance();
		this.preferences = MAVPreferences.getInstance();

		this.qnh = new MetarQNHService(MAVPreferences.getInstance().get(MAVPreferences.ICAO, "EDDM")).getQNH();

		state.getConnectedProperty().addListener((e,o,n) -> {
			if(!n.booleanValue()) {
				is_reading = false; 
				wq.removeTask("LP", timeout);
				state.getProgressProperty().set(StateProperties.NO_PROGRESS);
				if(!preferences.getBoolean(MAVPreferences.AUTOSAVE, false)) {
					parameterList.clear();
					state.getParamLoadedProperty().set(false);
				}
			} else {
				wq.addSingleTask("LP",1500, () -> refreshParameterList(true));
			}
		});
		
		state.getArmedProperty().addListener((e,o,n) -> {
			if(!n.booleanValue() && !state.getParamLoadedProperty().get()) {
				wq.addSingleTask("LP",1500, () -> refreshParameterList(true));
			}
		});

	}

	public void clear() {
		state.getParamLoadedProperty().set(false);
		parameterList.clear();
		property.setValue(null);
		wq.removeTask("LP", timeout);
	}

	public void refreshParameterList(boolean loaded) {
		if(!is_reading && !control.getCurrentModel().sys.isStatus(Status.MSP_ARMED) && !state.getLogLoadedProperty().get()) {
			is_reading = true;
			property.setValue(null);
			parameterList.clear();
			msg_param_request_list msg = new msg_param_request_list(255,1);
			msg.target_component = 1;
			msg.target_system = 1;
			control.sendMAVLinkMessage(msg);
			state.getParamLoadedProperty().set(!loaded);
			MSPLogger.getInstance().writeLocalMsg("Reading parameters...",
					MAV_SEVERITY.MAV_SEVERITY_INFO);

			wq.removeTask("LP", timeout);
			timeout = wq.addSingleTask("LP", 20000, () -> {
				state.getParamLoadedProperty().set(false);
				state.getProgressProperty().set(StateProperties.NO_PROGRESS);
				MSPLogger.getInstance().writeLocalMsg("Timeout reading parameters",
						MAV_SEVERITY.MAV_SEVERITY_WARNING);
				is_reading = false;
			});
		} 
	}


	public ObjectProperty<ParameterAttributes> getAttributeProperty() {
		return property;
	}

	@Override
	public void received(Object _msg) {

		if( _msg instanceof msg_param_value ) { //&& is_reading) {

			long flight_time = 0; double val;

			msg_param_value msg = (msg_param_value)_msg;

			if(msg.param_id[0]=='_')
				return;

			val = 	ParamUtils.paramToVal(msg.param_type, msg.param_value);

			ParameterAttributes attributes = metadata.getMetaData(msg.getParam_id());
			if(attributes == null)
				attributes = new ParameterAttributes(msg.getParam_id(),"Default Group");
			

			//if(attributes.value != val) {

				property.setValue(null);
				attributes.value = val;
				attributes.vtype = msg.param_type;

				parameterList.put(attributes.name,attributes);
				property.setValue(attributes);

			//}


			if(is_reading)
				state.getProgressProperty().set((float)msg.param_index/msg.param_count);

			if(msg.param_index >= msg.param_count-1) {
				wq.removeTask("LP", timeout);
				state.getParamLoadedProperty().set(true);
				state.getProgressProperty().set(StateProperties.NO_PROGRESS);
				for(IPX4ParameterRefresh l : refreshListeners)
					l.refresh();
				is_reading = false;

				// Flight time
				if(get("LND_FLIGHT_T_LO")!=null && get("LND_FLIGHT_T_HI") !=null ) {
					flight_time = (((long)get("LND_FLIGHT_T_HI").value << 32 ) + (long)get("LND_FLIGHT_T_LO").value);
					if(flight_time <1e10f && flight_time > 0)
						MSPLogger.getInstance().writeLocalMsg(String.format("Total flight time: %5.2f min", flight_time/60e6f),
								MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				}

				// Baro QNH check
//				if(qnh>0 && get("SENS_BARO_QNH").value!=qnh) {
//					MSPLogger.getInstance().writeLocalMsg("Baro QNH updated with "+qnh+", requires reboot.",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
//					sendParameter("SENS_BARO_QNH",qnh);
//				}
			}
		}
	}

	public void setParametersFromLog(Map<String,Object> list) {
		state.getParamLoadedProperty().set(false);
		parameterList.clear();
		list.forEach((s,o) -> {
			ParameterAttributes attributes = metadata.getMetaData(s);
			if(attributes == null)
				attributes = new ParameterAttributes(s,"Default Group");

			if(o instanceof Float)
				attributes.value = ((Float)(o)).floatValue();
			if(o instanceof Integer)
				attributes.value = ((Integer)(o)).floatValue();

			parameterList.put(attributes.name,attributes);
			property.setValue(attributes);
		});
		Platform.runLater(() -> {
			state.getParamLoadedProperty().set(true);
			for(IPX4ParameterRefresh l : refreshListeners)
				l.refresh();
		});
	}

	public void addRefreshListener(IPX4ParameterRefresh listener) {
		refreshListeners.add(listener);
	}


	public ParameterFactMetaData getMetaData() {
		return this.metadata;
	}

	public Map<String,ParameterAttributes> get() {
		return this.parameterList;
	}

	public void set(Map<String,ParameterAttributes> list) {
		StateProperties.getInstance().getParamLoadedProperty().set(false);
		parameterList.clear();
		list.forEach((s,o) -> {
			parameterList.put(o.name,o);
			property.setValue(o);
		});
		Platform.runLater(() -> {
			state.getParamLoadedProperty().set(true);
			for(IPX4ParameterRefresh l : refreshListeners)
				l.refresh();
		});
	}

	public List<ParameterAttributes> getList() {
		return asSortedList(this.parameterList);
	}

	public List<ParameterAttributes> getChanged() {
		List<ParameterAttributes> list = new ArrayList<ParameterAttributes>();
		parameterList.forEach((s,o) -> {
			double diff = BigDecimal.valueOf(o.value - o.default_val).setScale(o.decimals, RoundingMode.HALF_UP).doubleValue();
			if(( diff != 0 && o.default_val!=0 ) || ( o.default_val==0 && o.value != 0 ) && o.value!=2143289344)
				list.add(o);
		});
		java.util.Collections.sort(list, new Comparator<ParameterAttributes>() {
			@Override
			public int compare(ParameterAttributes o1, ParameterAttributes o2) {
				return o1.name.compareTo(o2.name);
			}
		});
		return list;
	}

	public ParameterAttributes get(String name) {
		return parameterList.get(name);
	}

	private List<ParameterAttributes> asSortedList(Map<String,ParameterAttributes> c) {
		List<ParameterAttributes> list = new ArrayList<ParameterAttributes>(c.values());
		java.util.Collections.sort(list, new Comparator<ParameterAttributes>() {
			@Override
			public int compare(ParameterAttributes o1, ParameterAttributes o2) {
				return o1.name.compareTo(o2.name);
			}
		});
		return list;
	}

}
