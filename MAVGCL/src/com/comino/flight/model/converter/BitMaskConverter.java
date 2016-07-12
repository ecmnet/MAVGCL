package com.comino.flight.model.converter;

public class BitMaskConverter extends SourceConverter {
	/*
	enum sensor_t {
		SENSOR_BARO = 0,
		SENSOR_GPS,
		SENSOR_LIDAR,
		SENSOR_FLOW,
		SENSOR_SONAR,
		SENSOR_VISION,
		SENSOR_MOCAP
	};
	*/
	
	

	@Override
	public float convert(float val) {
		if(((int)val >> (int)params[0] & 1) ==1 )
			return 1;
		return 0;
	}

	public BitMaskConverter() {
		super();
	}

}
