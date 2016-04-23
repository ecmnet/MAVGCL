package com.comino.flight.tabs.parameters;

import java.nio.ByteBuffer;

import org.mavlink.messages.MAV_PARAM_TYPE;

public class ParamUtils {


	public static float paramToVal (int type, float val)
	{
		if(type == MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32) {
			return (float) (ByteBuffer.allocate(4).putFloat(0,val).getInt(0));
		}
		else
			return val ;
	}

	public static float valToParam (int type, float val)
	{
		if(type == MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32) {
			return (float) (ByteBuffer.allocate(4).putInt(0,(int)val).getFloat(0));
		}
		else
			return val ;
	}

}
