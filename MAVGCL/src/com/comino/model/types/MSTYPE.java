package com.comino.model.types;

import java.util.ArrayList;

import com.comino.msp.model.DataModel;

public enum MSTYPE {

	MSP_NONE 		("none",1,""),
	MSP_ACCX 		("AccX",1,"m/s2"),
	MSP_ACCY 		("AccY",1,"m/s2"),
	MSP_ACCZ 		("AccZ",1,"m/s2"),
	MSP_GYROX		("GyroX",1,"rad/s"),
	MSP_GYROY		("GyroY",1,"rad/s"),
	MSP_GYROZ		("GyroZ",1,"rad/s"),
	MSP_MAGX		("MagX",1,"mT"),
	MSP_MAGY		("MagY",1,"mT"),
	MSP_MAGZ		("MagZ",1,"mT"),
	MSP_ANGLEX		("AngleX",1,"rad"),
	MSP_ANGLEY		("AngleY",1,"rad"),
	MSP_GRSPEED     ("Groundspeed",1,"m/s"),
	MSP_CLIMBRATE   ("Climb.Rate",1,"m/s"),
	MSP_COMPASS		("Compass",1,"°"),
	MSP_ALTLOCAL	("Alt.local",1,"m"),
	MSP_ALTAMSL		("Alt.amsl",1,"m"),
	MSP_ALTTERRAIN  ("Alt.terrain",1,"m"),
	MSP_NEDX		("Loc.PosX",1,"m"),
	MSP_NEDY		("Loc.PosY",1,"m"),
	MSP_NEDZ		("Loc.PosZ",1,"m"),
	MSP_NEDVX		("Loc.SpeedX",1,"m/s"),
	MSP_NEDVY		("Loc.SpeedY",1,"m/s"),
	MSP_NEDVZ		("Loc.SpeedZ",1,"m/s"),
	MSP_SPNEDX		("Sp.Loc.PosX",1,"m"),
	MSP_SPNEDY		("Sp.Loc.PosY",1,"m"),
	MSP_SPNEDZ		("Sp.Loc.PosZ",1,"m"),
	MSP_SPNEDVX		("Sp.Loc.SpeedX",1,"m/s"),
	MSP_SPNEDVY		("Sp.Loc.SpeedY",1,"m/s"),
	MSP_SPNEDVZ		("Sp.Loc.SpeedZ",1,"m/s"),
	MSP_LERRX		("Loc.ErrX",1,"m"),
	MSP_LERRY		("Loc.ErrY",1,"m"),
	MSP_LERRZ		("Loc.ErrZ",1,"m"),
	MSP_RNEDX		("Loc.PosX.rel.",1,"m"),
	MSP_RNEDY		("Loc.PosY.rel.",1,"m"),
	MSP_RNEDZ		("Loc.PosZ.rel.",1,"m"),
	MSP_RAW_DI		("Raw.Distance.",1,"m"),
	MSP_DEBUGX		("DebugX",1,"-"),
	MSP_DEBUGY		("DebugY",1,"-"),
	MSP_DEBUGZ		("DebugZ",1,"-"),
	MSP_DEBUGH		("DebugH",1,"-"),
	MSP_RAW_FLOWX	("Raw.FlowX",1,""),
	MSP_RAW_FLOWY	("Raw.FlowY",1,""),
	MSP_RAW_FLOWQ	("Raw.Flow.Qual.",1,""),
	MSP_RAW_FLOWD	("Raw.Flow.Dist.",1,"m"),
	MSP_VOLTAGE		("Bat.Voltage",1,"V"),
	MSP_CURRENT		("Bat.Current",1,"A"),
	MSP_RC0			("RC.0",1,""),
	MSP_RC1			("RC.1",1,""),
	MSP_RC2			("RC.2",1,""),
	MSP_RC3			("RC.3",1,""),
	MSP_S0			("Servo.0",0,""),
	MSP_S1			("Servo.1",0,""),
	MSP_S2			("Servo.2",0,""),
	MSP_S3			("Servo.3",0,""),
	MSP_GLOBRELX	("Global.rel.PosX",1,"m"),
	MSP_GLOBRELY 	("Global.rel.PosY",1,"m"),
	MSP_GLOBRELZ	("Global.rel.PosZ",1,"m"),
	MSP_GLOBRELVX	("Global.rel.SpeedX",1,"m/s"),
	MSP_GLOBRELVY   ("Global.rel.SpeedY",1,"m/s"),
	MSP_GLOBRELVZ	("Global.rel.SpeedZ",1,"m/s"),
	MSP_GPSEPH  	("GPS.eph",1,""),
	MSP_GLOBPLAT	("Global.Latitude",0,"°"),
	MSP_GLOBPLON	("Global.Longitude",0,"°"),
	MSP_RAW_GPSLAT  ("Raw.Latitude",0,"°"),
	MSP_RAW_GPSLON  ("Raw.Longitude",0,"°"),
	MSP_REF_GPSLAT  ("Home.Latitude",0,"°"),
	MSP_REF_GPSLON  ("Home.Longitude",0,"°"),
	MSP_CONSPOWER	("Bat.Cons.Power",0,"mAh"),
	MSP_RAW_SATNUM  ("Satellites",1,""),
	MSP_VIBX        ("VibrationX",1,""),
	MSP_VIBY        ("VibrationY",1,""),
	MSP_VIBZ        ("VibrationZ",1,""),
	MSP_ABSPRESSURE ("Abs.Pressure",0,"hPa"),
	MSP_IMUTEMP	    ("Imu.Temp.",0,"°C"),
	MSP_CPULOAD     ("CPU Load",0,"°%"),

	;

	private final String description;
	private final int    type;
	private final String unit;

	private static ArrayList<String> list = new ArrayList<String>();

	static {
		for(MSTYPE mstype : MSTYPE.values()) {
			if(mstype.type==1)
				list.add(mstype.description);
		}
	}


	private MSTYPE(String s, int type, String unit) {
		this.description = s;
		this.type = type;
		this.unit = unit;
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
		case MSP_ANGLEX: 		return m.attitude.aX;
		case MSP_ANGLEY: 		return m.attitude.aY;
		case MSP_GRSPEED: 		return m.attitude.s;
		case MSP_CLIMBRATE:     return m.attitude.vs;
		case MSP_COMPASS: 		return m.attitude.h;
		case MSP_ALTLOCAL: 		return m.attitude.al;
		case MSP_ALTAMSL: 		return m.attitude.ag;
		case MSP_ALTTERRAIN:	return m.attitude.at;
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
		case MSP_RAW_FLOWD:     return m.raw.fd;
		case MSP_VOLTAGE:	    return m.battery.b0;
		case MSP_CURRENT:	    return m.battery.c0;
		case MSP_CONSPOWER:     return m.battery.a0;
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
		case MSP_GPSEPH: 		return (float) m.gps.eph;
		case MSP_RAW_SATNUM:    return m.gps.numsat;
		case MSP_VIBX:          return m.vibration.vibx;
		case MSP_VIBY:          return m.vibration.viby;
		case MSP_VIBZ:          return m.vibration.vibz;
		case MSP_ABSPRESSURE:   return m.imu.abs_pressure;
		case MSP_IMUTEMP:       return m.sys.imu_temp;
		case MSP_CPULOAD:       return m.sys.load_p;


		default:
			return -1;
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
