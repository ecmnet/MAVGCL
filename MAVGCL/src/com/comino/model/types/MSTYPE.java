/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.model.types;

import java.util.ArrayList;

import com.comino.msp.model.DataModel;

public enum MSTYPE {

	MSP_NONE 		("none",1,"",""),
	MSP_ACCX 		("AccX",1,"m/s2","IMU.AccX"),
	MSP_ACCY 		("AccY",1,"m/s2","IMU.AccY"),
	MSP_ACCZ 		("AccZ",1,"m/s2","IMU.AccZ"),
	MSP_GYROX		("GyroX",1,"rad/s","IMU.GyroX"),
	MSP_GYROY		("GyroY",1,"rad/s","IMU.GyroY"),
	MSP_GYROZ		("GyroZ",1,"rad/s","IMU.GyroZ"),
	MSP_MAGX		("MagX",1,"mT","IMU.MagX"),
	MSP_MAGY		("MagY",1,"mT","IMU.MagY"),
	MSP_MAGZ		("MagZ",1,"mT","IMU.MagZ"),
	MSP_ATTROLL     ("Roll",1,"°","ATT.Roll"),
	MSP_ATTPITCH    ("Pitch",1,"°","ATT.Pitch"),
	MSP_ATTYAW      ("Yaw",1,"°","ATT.Yaw"),
	MSP_ATTROLL_R   ("RollRate",1,"°/s",""),
	MSP_ATTPITCH_R  ("PitchRate",1,"°/s",""),
	MSP_ATTYAW_R    ("YawRate",1,"°/s",""),
	MSP_SPATTROLL   ("Sp.Roll",1,"°",""),
	MSP_SPATTPIT    ("Sp.Pitch",1,"°",""),
	MSP_SPATTYAW    ("Sp.Yaw",1,"°",""),
	MSP_SPATTROLL_R ("Sp.RollRate",1,"°/s",""),
	MSP_SPATTPIT_R  ("Sp.PitchRate",1,"°/s",""),
	MSP_SPATTYAW_R  ("Sp.YawRate",1,"°/s",""),
	MSP_GRSPEED     ("Groundspeed",1,"m/s",""),
	MSP_CLIMBRATE   ("Climb.Rate",1,"m/s",""),
	MSP_COMPASS		("Compass",1,"°",""),
	MSP_ALTLOCAL	("Alt.local",1,"m",""),
	MSP_ALTAMSL		("Alt.amsl",1,"m","GPOS.Alt"),
	MSP_ALTTERRAIN  ("Alt.terrain",1,"m","GPOS.TALT"),
	MSP_NEDX		("Loc.PosX",1,"m","LPOS.X"),
	MSP_NEDY		("Loc.PosY",1,"m","LPOS.Y"),
	MSP_NEDZ		("Loc.PosZ",1,"m","LPOS.Z"),
	MSP_NEDVX		("Loc.SpeedX",1,"m/s","LPOS.VX"),
	MSP_NEDVY		("Loc.SpeedY",1,"m/s","LPOS.VY"),
	MSP_NEDVZ		("Loc.SpeedZ",1,"m/s","LPOS.VZ"),
	MSP_SPNEDX		("Sp.Loc.PosX",1,"m","LPSP.X"),
	MSP_SPNEDY		("Sp.Loc.PosY",1,"m","LPSP.Y"),
	MSP_SPNEDZ		("Sp.Loc.PosZ",1,"m","LPSP.Z"),
	MSP_SPNEDVX		("Sp.Loc.SpeedX",1,"m/s","LPSP.VX"),
	MSP_SPNEDVY		("Sp.Loc.SpeedY",1,"m/s","LPSP.VY"),
	MSP_SPNEDVZ		("Sp.Loc.SpeedZ",1,"m/s","LPSP.VZ"),
	MSP_NEDAX		("Loc.AccX",1,"m/s^2",""),
	MSP_NEDAY		("Loc.AccY",1,"m/s^2",""),
	MSP_NEDAZ		("Loc.AccZ",1,"m/s^2",""),
	MSP_RAW_DI		("Raw.Distance.",1,"m","DIST.Distance"),
	MSP_DEBUGX		("DebugX",1,"-",""),
	MSP_DEBUGY		("DebugY",1,"-",""),
	MSP_DEBUGZ		("DebugZ",1,"-",""),
	MSP_DEBUGH		("DebugH",1,"-",""),
	MSP_RAW_FLOWX	("Raw.FlowX",1,"","FLOW.RawX"),
	MSP_RAW_FLOWY	("Raw.FlowY",1,"","FLOW.RawY"),
	MSP_RAW_FLOWQ	("Raw.Flow.Qual.",1,"","FLOW.Qlty"),
	MSP_RAW_FLOWD	("Raw.Flow.Dist.",1,"m","FLOW.Dist"),
	MSP_VOLTAGE		("Bat.Voltage",1,"V","BATT.V"),
	MSP_CURRENT		("Bat.Current",1,"A","BATT.C"),
	MSP_RC0			("RC.0",1,"","RC.C0"),
	MSP_RC1			("RC.1",1,"","RC.C1"),
	MSP_RC2			("RC.2",1,"","RC.C2"),
	MSP_RC3			("RC.3",1,"","RC.C3"),
	MSP_RSSI        ("RSSI",1,"",""),
	MSP_GLOBRELVX	("Global.rel.SpeedX",1,"cm/s","GPOS.VelN"),
	MSP_GLOBRELVY   ("Global.rel.SpeedY",1,"cm/s","GPOS.VelE"),
	MSP_GLOBRELVZ	("Global.rel.SpeedZ",1,"cm/s","GPOS.VelD"),
	MSP_GPSEPH  	("GPS.eph",1,"","GPS.EPH"),
	MSP_GPSHDOP     ("GPS.hdop",1,"","GPS.HDOP"),
	MSP_RAW_SATNUM  ("Satellites",1,"","GPS.nSat"),
	MSP_ACT0		("Actuator.0",1,"",""),
	MSP_ACT1		("Actuator.1",1,"",""),
	MSP_ACT2		("Actuator.2",1,"",""),
	MSP_ACT3		("Actuator.3",1,"",""),
	MSP_SPACT0		("Sp.Actuat.0",1,"",""),
	MSP_SPACT1		("Sp.Actuat.1",1,"",""),
	MSP_SPACT2		("Sp.Actuat.2",1,"",""),
	MSP_SPACT3		("Sp.Actuat.3",1,"",""),
	MSP_VIBX        ("VibrationX",1,"",""),
	MSP_VIBY        ("VibrationY",1,"",""),
	MSP_VIBZ        ("VibrationZ",1,"",""),


	// now  keyfigures that are not selectable

	MSP_ANGLEX		("AngleX",0,"rad","ATT.Roll"),
	MSP_ANGLEY		("AngleY",0,"rad","ATT.Pitch"),
	MSP_ABSPRESSURE ("Abs.Pressure",0,"hPa",""),
	MSP_IMUTEMP	    ("Imu.Temp.",0,"°C",""),
	MSP_CPULOAD     ("CPU Load",0,"%",""),
	MSP_GLOBPLAT	("Global.Latitude",0,"°","GPOS.Lat"),
	MSP_GLOBPLON	("Global.Longitude",0,"°","GPOS.Lon"),
	MSP_RAW_GPSLAT  ("Raw.Latitude",0,"°","GPOS.Lat"),
	MSP_RAW_GPSLON  ("Raw.Longitude",0,"°","GPOS.Lon"),
	MSP_HOME_LAT    ("Home.Latitude",0,"°",""),
	MSP_HOME_LON    ("Home.Longitude",0,"°",""),
	MSP_HOME_ALT    ("Home.Altitude",0,"m",""),
	MSP_CONSPOWER	("Bat.Cons.Power",0,"mAh",""),
	MSP_TIME_ARMED  ("Time armed",0,"s","");

	;

	private final String description;
	private final int    type;
	private final String unit;
	private final String px4logname;

	private static ArrayList<String> list = new ArrayList<String>();

	static {
		for(MSTYPE mstype : MSTYPE.values()) {
			if(mstype.type==1)
				list.add(mstype.description);
		}
	}


	private MSTYPE(String s, int type, String unit, String px4logname) {
		this.description = s;
		this.type = type;
		this.unit = unit;
		this.px4logname = px4logname;
	}

	public static String getPX4LogName(MSTYPE mstype) {
		return mstype.px4logname;
	}

	public static float getValue(DataModel m, MSTYPE mstype) {
		switch(mstype) {
		case MSP_ACCX: 			return m.imu.accx;
		case MSP_ACCY: 			return m.imu.accy;
		case MSP_ACCZ: 			return m.imu.accz;
		case MSP_GYROX: 		return m.imu.gyrox;
		case MSP_GYROY: 		return m.imu.gyroy;
		case MSP_GYROZ: 		return m.imu.gyroz;
		case MSP_MAGX: 			return m.imu.magx;
		case MSP_MAGY: 			return m.imu.magy;
		case MSP_MAGZ: 			return m.imu.magz;
		case MSP_DEBUGX: 		return m.debug.x;
		case MSP_DEBUGY: 		return m.debug.y;
		case MSP_DEBUGZ: 		return m.debug.z;
		case MSP_DEBUGH: 		return m.debug.h;
		case MSP_ATTROLL:       return m.attitude.r;
		case MSP_ATTPITCH:      return m.attitude.p;
		case MSP_ATTYAW:        return m.attitude.y;
		case MSP_ATTROLL_R:     return m.attitude.rr;
		case MSP_ATTPITCH_R:    return m.attitude.pr;
		case MSP_ATTYAW_R:      return m.attitude.yr;
		case MSP_SPATTROLL:     return m.attitude.sr;
		case MSP_SPATTPIT:      return m.attitude.sp;
		case MSP_SPATTYAW:      return m.attitude.sy;
		case MSP_SPATTROLL_R:   return m.attitude.srr;
		case MSP_SPATTPIT_R:    return m.attitude.spr;
		case MSP_SPATTYAW_R:    return m.attitude.syr;
		case MSP_ANGLEX: 		return m.hud.aX;
		case MSP_ANGLEY: 		return m.hud.aY;
		case MSP_GRSPEED: 		return m.hud.s;
		case MSP_CLIMBRATE:     return m.hud.vs;
		case MSP_COMPASS: 		return m.hud.h;
		case MSP_ALTLOCAL: 		return m.hud.al;
		case MSP_ALTAMSL: 		return m.hud.ag;
		case MSP_ALTTERRAIN:	return m.hud.at;
		case MSP_NEDX:			return m.state.l_x;
		case MSP_NEDY:			return m.state.l_y;
		case MSP_NEDZ:			return m.state.l_z;
		case MSP_NEDVX:			return m.state.l_vx;
		case MSP_NEDVY:			return m.state.l_vy;
		case MSP_NEDVZ:			return m.state.l_vz;
		case MSP_SPNEDX:		return m.target_state.l_x;
		case MSP_SPNEDY:		return m.target_state.l_y;
		case MSP_SPNEDZ:		return m.target_state.l_z;
		case MSP_SPNEDVX:		return m.target_state.l_vx;
		case MSP_SPNEDVY:		return m.target_state.l_vy;
		case MSP_SPNEDVZ:		return m.target_state.l_vz;
		case MSP_NEDAX:		    return m.state.l_ax;
		case MSP_NEDAY:		    return m.state.l_ay;
		case MSP_NEDAZ:		    return m.state.l_az;
		case MSP_RC0:			return m.rc.s0;
		case MSP_RC1:			return m.rc.s1;
		case MSP_RC2:			return m.rc.s2;
		case MSP_RC3:			return m.rc.s3;
		case MSP_RSSI:			return m.rc.rssi;
		case MSP_RAW_DI:	    return m.raw.di;
		case MSP_RAW_FLOWX:	    return m.raw.fX;
		case MSP_RAW_FLOWY:	    return m.raw.fY;
		case MSP_RAW_FLOWQ:     return m.raw.fq;
		case MSP_RAW_FLOWD:     return m.raw.fd;
		case MSP_VOLTAGE:	    return m.battery.b0;
		case MSP_CURRENT:	    return m.battery.c0;
		case MSP_CONSPOWER:     return m.battery.a0;
		case MSP_ACT0:			return m.servo.actuators[0];
		case MSP_ACT1:			return m.servo.actuators[1];
		case MSP_ACT2:			return m.servo.actuators[2];
		case MSP_ACT3:			return m.servo.actuators[3];
		case MSP_SPACT0:	    return m.servo.controls[0];
		case MSP_SPACT1:     	return m.servo.controls[1];
		case MSP_SPACT2:		return m.servo.controls[2];
		case MSP_SPACT3:		return m.servo.controls[3];
		case MSP_GLOBRELVX:		return m.state.g_vx;
		case MSP_GLOBRELVY:		return m.state.g_vy;
		case MSP_GLOBRELVZ:		return m.state.g_vz;
		case MSP_GLOBPLAT:		return m.state.g_lat;
		case MSP_GLOBPLON:		return m.state.g_lon;
		case MSP_RAW_GPSLAT:	return (float) m.gps.latitude;
		case MSP_RAW_GPSLON:	return (float) m.gps.longitude;
		case MSP_HOME_LAT:      return (float) m.home_state.g_lat;
		case MSP_HOME_LON:      return (float) m.home_state.g_lon;
		case MSP_HOME_ALT:      return (float) m.home_state.g_alt;
		case MSP_GPSEPH: 		return (float) m.gps.eph;
		case MSP_GPSHDOP: 		return (float) m.gps.hdop;
		case MSP_RAW_SATNUM:    return m.gps.numsat;
		case MSP_VIBX:          return m.vibration.vibx;
		case MSP_VIBY:          return m.vibration.viby;
		case MSP_VIBZ:          return m.vibration.vibz;
		case MSP_ABSPRESSURE:   return m.imu.abs_pressure;
		case MSP_IMUTEMP:       return m.sys.imu_temp;
		case MSP_CPULOAD:       return m.sys.load_p;
		case MSP_TIME_ARMED:    return m.sys.t_armed_ms / 1000;


		default:
			return -1;
		}
	}

	public static void putValue(DataModel m, MSTYPE mstype, float value) {
		switch(mstype) {
		case MSP_ACCX: 			 m.imu.accx = value; break;
		case MSP_ACCY: 			 m.imu.accy = value; break;
		case MSP_ACCZ: 			 m.imu.accz = value; break;
		case MSP_GYROX: 		 m.imu.gyrox = value; break;
		case MSP_GYROY: 		 m.imu.gyroy = value; break;
		case MSP_GYROZ: 		 m.imu.gyroz = value; break;
		case MSP_MAGX: 		     m.imu.magx = value; break;
		case MSP_MAGY: 			 m.imu.magy = value; break;
		case MSP_MAGZ: 			 m.imu.magz = value; break;
		case MSP_DEBUGX: 		 m.debug.x = value; break;
		case MSP_DEBUGY: 		 m.debug.y = value; break;
		case MSP_DEBUGZ: 		 m.debug.z = value; break;
		case MSP_DEBUGH: 		 m.debug.h = value; break;
		case MSP_ATTROLL:       m.attitude.r = value; break;
		case MSP_ATTPITCH:      m.attitude.p = value; break;
		case MSP_ATTYAW:        m.attitude.y = value; break;
		case MSP_ATTROLL_R:     m.attitude.rr = value; break;
		case MSP_ATTPITCH_R:    m.attitude.pr = value; break;
		case MSP_ATTYAW_R:      m.attitude.yr = value; break;
		case MSP_SPATTROLL:     m.attitude.sr = value; break;
		case MSP_SPATTPIT:      m.attitude.sp = value; break;
		case MSP_SPATTYAW:      m.attitude.sy = value; break;
		case MSP_SPATTROLL_R:   m.attitude.srr = value; break;
		case MSP_SPATTPIT_R:    m.attitude.spr = value; break;
		case MSP_SPATTYAW_R:    m.attitude.syr = value; break;
		case MSP_ANGLEX: 		 m.hud.aX = value; break;
		case MSP_ANGLEY: 		 m.hud.aY = value; break;
		case MSP_GRSPEED: 		 m.hud.s = value; break;
		case MSP_CLIMBRATE:      m.hud.vs = value; break;
		case MSP_COMPASS: 		 m.hud.h = value; break;
		case MSP_ALTLOCAL: 		 m.hud.al = value; break;
		case MSP_ALTAMSL: 		 m.hud.ag = value; break;
		case MSP_ALTTERRAIN:	 m.hud.at = value; break;
		case MSP_NEDX:			 m.state.l_x = value; break;
		case MSP_NEDY:			 m.state.l_y = value; break;
		case MSP_NEDZ:			 m.state.l_z = value; break;
		case MSP_NEDVX:			 m.state.l_vx = value; break;
		case MSP_NEDVY:			 m.state.l_vy = value; break;
		case MSP_NEDVZ:			 m.state.l_vz = value; break;
		case MSP_NEDAX:			 m.state.l_vx = value; break;
		case MSP_NEDAY:			 m.state.l_vy = value; break;
		case MSP_NEDAZ:			 m.state.l_vz = value; break;
		case MSP_SPNEDX:		 m.target_state.l_x = value; break;
		case MSP_SPNEDY:		 m.target_state.l_y = value; break;
		case MSP_SPNEDZ:		 m.target_state.l_z = value; break;
		case MSP_SPNEDVX:		 m.target_state.l_vx = value; break;
		case MSP_SPNEDVY:		 m.target_state.l_vy = value; break;
		case MSP_SPNEDVZ:		 m.target_state.l_vz = value; break;
		case MSP_RC0:			 m.rc.s0 = (short) value; break;
		case MSP_RC1:			 m.rc.s1 = (short) value; break;
		case MSP_RC2:			 m.rc.s2 = (short) value; break;
		case MSP_RC3:			 m.rc.s3 = (short) value; break;
		case MSP_RSSI:           m.rc.rssi = (short) value; break;
		case MSP_RAW_DI:	     m.raw.di = value; break;
		case MSP_RAW_FLOWX:	     m.raw.fX = value; break;
		case MSP_RAW_FLOWY:	     m.raw.fY = value; break;
		case MSP_RAW_FLOWQ:      m.raw.fq = (int) value; break;
		case MSP_RAW_FLOWD:      m.raw.fd = value; break;
		case MSP_VOLTAGE:	     m.battery.b0 = value; break;
		case MSP_CURRENT:	     m.battery.c0 = value; break;
		case MSP_CONSPOWER:      m.battery.a0 = value; break;
		case MSP_ACT0:			 m.servo.actuators[0] = value; break;
		case MSP_ACT1:		     m.servo.actuators[1] = value; break;
		case MSP_ACT2:			 m.servo.actuators[2] = value; break;
		case MSP_ACT3:			 m.servo.actuators[3] = value; break;
		case MSP_SPACT0:		 m.servo.controls[0] = value; break;
		case MSP_SPACT1:	     m.servo.controls[1] = value; break;
		case MSP_SPACT2:		 m.servo.controls[2] = value; break;
		case MSP_SPACT3:		 m.servo.controls[3] = value; break;
		case MSP_GLOBRELVX:		 m.state.g_vx = value; break;
		case MSP_GLOBRELVY:		 m.state.g_vy = value; break;
		case MSP_GLOBRELVZ:		 m.state.g_vz = value; break;
		case MSP_GLOBPLAT:		 m.state.g_lat = value; break;
		case MSP_GLOBPLON:		 m.state.g_lon = value; break;
		case MSP_RAW_GPSLAT:	 m.gps.latitude = value; break;
		case MSP_RAW_GPSLON:	 m.gps.longitude = value; break;
		case MSP_HOME_LAT:       m.home_state.g_lat = value; break;
		case MSP_HOME_LON:       m.home_state.g_lon = value; break;
		case MSP_HOME_ALT:       m.home_state.g_alt = value; break;
		case MSP_GPSEPH: 		 m.gps.eph = value; break;
		case MSP_GPSHDOP: 		 m.gps.hdop = value; break;
		case MSP_RAW_SATNUM:     m.gps.numsat = (byte) value; break;
		case MSP_VIBX:           m.vibration.vibx = value; break;
		case MSP_VIBY:           m.vibration.viby = value; break;
		case MSP_VIBZ:           m.vibration.vibz = value; break;
		case MSP_ABSPRESSURE:    m.imu.abs_pressure = value; break;
		case MSP_IMUTEMP:        m.sys.imu_temp = (int) value; break;
		case MSP_CPULOAD:        m.sys.load_p = value; break;
		case MSP_TIME_ARMED:     m.sys.t_armed_ms = (long)(value * 1000); break;

		}
	}

	public String getDescription() {
		return description;
	}

	public String getUnit() {
		return unit;
	}


	public static String getDescriptionOf(int index) {
		return MSTYPE.values()[index].description;
	}

	public static MSTYPE getTypeOf(int index) {
		return MSTYPE.values()[index];
	}

	public static String[] getList() {
		return list.toArray(new String[list.size()]);
	}


	public static void main(String[] args) {
		for(String ms : MSTYPE.getList())
			System.out.println(ms);

		System.out.println(MSTYPE.getDescriptionOf(2));
	}


}
