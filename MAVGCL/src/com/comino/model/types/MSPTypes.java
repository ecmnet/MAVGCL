/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.model.types;

import com.comino.msp.model.DataModel;

public class MSPTypes {

	public static final int	MSP_ACCX		= 1;
	public static final int	MSP_ACCY		= 2;
	public static final int	MSP_ACCZ		= 3;
	public static final int	MSP_GYROX		= 4;
	public static final int	MSP_GYROY		= 5;
	public static final int	MSP_GYROZ		= 6;
	public static final int	MSP_ANGLEX		= 7;
	public static final int	MSP_ANGLEY		= 8;
	public static final int	MSP_COMPASS	    = 9;
	public static final int	MSP_AL	   		= 10;
	public static final int	MSP_AS	   		= 11;

	public static final int	MSP_NEDX		= 12;
	public static final int	MSP_NEDY		= 13;
	public static final int	MSP_NEDZ		= 14;
	public static final int	MSP_NEDVX		= 15;
	public static final int	MSP_NEDVY		= 16;
	public static final int	MSP_NEDVZ		= 17;

	public static final int	MSP_SPNEDX		= 18;
	public static final int	MSP_SPNEDY		= 19;
	public static final int	MSP_SPNEDZ		= 20;
	public static final int	MSP_SPNEDVX		= 21;
	public static final int	MSP_SPNEDVY		= 22;
	public static final int	MSP_SPNEDVZ		= 23;

	public static final int	MSP_LERRX		= 24;
	public static final int	MSP_LERRY		= 25;
	public static final int	MSP_LERRZ		= 26;


	public static final int	MSP_RC0			= 27;
	public static final int	MSP_RC1			= 28;
	public static final int	MSP_RC2			= 29;
	public static final int	MSP_RC3			= 30;
	public static final int	MSP_RAW_DI		= 31;
	public static final int	MSP_RAW_FLOWX	= 32;
	public static final int	MSP_RAW_FLOWY   = 33;
	public static final int	MSP_RAW_FLOWQ   = 34;

	public static final int	MSP_VOLTAGE		= 35;
	public static final int	MSP_CURRENT   	= 36;





	private static final String type_names[]  = {
			"not selected",
			"AccX", "AccY", "AccZ",
			"GyroX", "GyroY","GyroZ",
			"AngleX", "AngleY","Compass","Alt.(local)","Alt.(amsl)",

			"Rel.locPosX", "Rel.locPosY", "Rel.locPosZ",
			"Loc.SpeedX", "Loc.SpeedY", "LocSpeedZ",
			"Sp.L.PosX", "Sp.L.PosY", "Sp.L.PosZ",
			"Sp.L.SpeedX", "Sp.L.SpeedY","Sp.L.SpeedZ",
			"L.Pos.ErrorX", "L.Pos.ErrorY", "L.Pos.ErrorZ",
			"RC Chan1.", "RC Chan2.", "RC Chan3.", "RC Chan4.",
			"Distance", "FlowX", "FlowY", "Flow Quality",
			"Voltage", "Current"
	};

	public static float getFloat(DataModel m, int type) {
		switch(type) {
		case MSP_ACCX: 			return m.imu.accy;
		case MSP_ACCY: 			return m.imu.accx;
		case MSP_ACCZ: 			return m.imu.accz;
		case MSP_GYROX: 		return m.imu.gyrox;
		case MSP_GYROY: 		return m.imu.gyroy;
		case MSP_GYROZ: 		return m.imu.gyroz;
		case MSP_ANGLEX: 		return m.attitude.aX;
		case MSP_ANGLEY: 		return m.attitude.aY;
		case MSP_COMPASS: 		return m.attitude.h;
		case MSP_AL: 			return m.attitude.al;
		case MSP_AS: 			return m.attitude.ag;
		case MSP_NEDX:			return m.state.x - m.state.hx;
		case MSP_NEDY:			return m.state.y - m.state.hy;
		case MSP_NEDZ:			return m.state.z - m.state.hz;
		case MSP_NEDVX:			return m.state.vx;
		case MSP_NEDVY:			return m.state.vy;
		case MSP_NEDVZ:			return m.state.vz;
		case MSP_SPNEDX:		return m.target_state.x;
		case MSP_SPNEDY:		return m.target_state.y;
		case MSP_SPNEDZ:		return m.target_state.z;
		case MSP_SPNEDVX:		return m.target_state.vx;
		case MSP_SPNEDVY:		return m.target_state.vy;
		case MSP_SPNEDVZ:		return m.target_state.vz;
		case MSP_LERRX:		    return m.state.x - m.target_state.x;
		case MSP_LERRY:		    return m.state.y - m.target_state.y;
		case MSP_LERRZ:		    return m.state.z - m.target_state.z;
		case MSP_RC0:			return m.rc.s0;
		case MSP_RC1:			return m.rc.s1;
		case MSP_RC2:			return m.rc.s2;
		case MSP_RC3:			return m.rc.s3;
		case MSP_RAW_DI:	    return m.raw.di;
		case MSP_RAW_FLOWX:	    return m.raw.fX;
		case MSP_RAW_FLOWY:	    return m.raw.fY;
		case MSP_RAW_FLOWQ:     return m.raw.fq;
		case MSP_VOLTAGE:	    return m.battery.b0;
		case MSP_CURRENT:	    return m.battery.c0;


		default: return 0;
		}
	}

	public static String[] getNames() {
		return type_names;
	}


	public static int getType(String type_name) {
		for(int i=0;i<type_names.length;i++) {
			if(type_name.equals(type_names[i]))
				return i;
		}
		return 0;
	}

}
