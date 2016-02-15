package com.comino.model.types;

import java.util.ArrayList;


import com.comino.msp.model.DataModel;

public enum MSTYPE {

	MSP_NONE 		("none"),
	MSP_ACCX 		("AccX"),
	MSP_ACCY 		("AccY"),
	MSP_ACCZ 		("AccZ"),
	MSP_GYROX		("GyroX"),
	MSP_GYROY		("GyroY"),
	MSP_GYROZ		("GyroZ"),
	MSP_ANGLEX		("AngleX"),
	MSP_ANGLEY		("AngleY"),
	MSP_COMPASS		("Compass"),
	MSP_AL			("Alt.local"),
	MSP_AS			("Alt.amsl"),
	MSP_NEDX		("Loc.PosX"),
	MSP_NEDY		("Loc.PosY"),
	MSP_NEDZ		("Loc.PosZ"),
	MSP_NEDVX		("Loc.SpeedX"),
	MSP_NEDVY		("Loc.SpeedY"),
	MSP_NEDVZ		("Loc.SpeedZ"),
	MSP_SPNEDX		("Sp.Loc.PosX"),
	MSP_SPNEDY		("Sp.Loc.PosY"),
	MSP_SPNEDZ		("Sp.Loc.PosZ"),
	MSP_SPNEDVX		("Sp.Loc.SpeedX"),
	MSP_SPNEDVY		("Sp.Loc.SpeedY"),
	MSP_SPNEDVZ		("Sp.Loc.SpeedZ"),
	MSP_LERRX		("Loc.ErrX"),
	MSP_LERRY		("Loc.ErrY"),
	MSP_LERRZ		("Loc.ErrZ"),
	MSP_RNEDX		("Loc.PosX.rel."),
	MSP_RNEDY		("Loc.PosX.rel."),
	MSP_RNEDZ		("Loc.PosX.rel."),
	MSP_RAW_DI		("Raw.Dist.S."),
	MSP_RAW_FLOWX	("Raw.FlowX"),
	MSP_RAW_FLOWY	("Raw.FlowX"),
	MSP_RAW_FLOWQ	("Raw.Flow.Qual."),
	MSP_VOLTAGE		("Bat.Voltage"),
	MSP_CURRENT		("Bat.Current"),
	MSP_RC0			("RC.0"),
	MSP_RC1			("RC.1"),
	MSP_RC2			("RC.2"),
	MSP_RC3			("RC.3"),
	MSP_S0			("Servo.0"),
	MSP_S1			("Servo.1"),
	MSP_S2			("Servo.2"),
	MSP_S3			("Servo.3"),
	MSP_GLOBRELX	("Global.rel.PosX"),
	MSP_GLOBRELY 	("Global.rel.PosY"),
	MSP_GLOBRELZ	("Global.rel.PosZ"),
	MSP_GLOBRELVX	("Global.rel.SpeedX"),
	MSP_GLOBRELVY   ("Global.rel.SpeedY"),
	MSP_GLOBRELVZ	("Global.rel.SpeedZ"),
	MSP_GLOBPLAT	("Global.Latitude"),
	MSP_GLOBPLON	("Global.Longitude"),
	MSP_RAW_GPSLAT  ("Raw.Latitude"),
	MSP_RAW_GPSLON  ("Raw.Longitude"),
	MSP_REF_GPSLAT  ("Home.Latitude"),
	MSP_REF_GPSLON  ("Home.Longitude"),

;

	private final String description;

	private static ArrayList<String> list = new ArrayList<String>();

	static {
		for(MSTYPE mstype : MSTYPE.values()) {
			list.add(mstype.description);
		}
	}


	private MSTYPE(String s) {
		this.description = s;
	}

	public static float getValue(DataModel m, MSTYPE mstype) {
		switch(mstype) {
		case MSP_ACCX: 			return m.imu.accx;
		case MSP_ACCY: 			return m.imu.accy;
		case MSP_ACCZ: 			return m.imu.accz;
		case MSP_GYROX: 		return m.imu.gyrox;
		case MSP_GYROY: 		return m.imu.gyroy;
		case MSP_GYROZ: 		return m.imu.gyroz;
		case MSP_ANGLEX: 		return m.attitude.aX;
		case MSP_ANGLEY: 		return m.attitude.aY;
		case MSP_COMPASS: 		return m.attitude.h;
		case MSP_AL: 			return m.attitude.al;
		case MSP_AS: 			return m.attitude.ag;
		case MSP_NEDX:			return m.state.x;
		case MSP_NEDY:			return m.state.y;
		case MSP_NEDZ:			return m.state.z;
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
		case MSP_RNEDX:			return m.state.x - m.state.hx;
		case MSP_RNEDY:			return m.state.y - m.state.hy;
		case MSP_RNEDZ:			return m.state.z - m.state.hz;
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
		case MSP_S0:			return m.servo.s0;
		case MSP_S1:			return m.servo.s1;
		case MSP_S2:			return m.servo.s2;
		case MSP_S3:			return m.servo.s3;
		case MSP_GLOBRELX:		return m.state.g_x;
		case MSP_GLOBRELY:		return m.state.g_y;
		case MSP_GLOBRELZ:		return m.state.g_z;
		case MSP_GLOBRELVX:		return m.state.g_vx;
		case MSP_GLOBRELVY:		return m.state.g_vy;
		case MSP_GLOBRELVZ:		return m.state.g_vz;
		case MSP_GLOBPLAT:		return m.state.lat;
		case MSP_GLOBPLON:		return m.state.lon;
		case MSP_RAW_GPSLAT:	return (float) m.gps.latitude;
		case MSP_RAW_GPSLON:	return (float) m.gps.longitude;
		case MSP_REF_GPSLAT:    return (float) m.gps.ref_lat;
		case MSP_REF_GPSLON:    return (float) m.gps.ref_lon;
		default:
			return 0;
		}
	}

	public String getDescription() {
		return description;
	}

	public static String getDescriptionOf(int index) {
		return MSTYPE.values()[index].description;
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
