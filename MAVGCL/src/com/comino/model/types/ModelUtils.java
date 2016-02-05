package com.comino.model.types;

public class ModelUtils {

	private static final double toRad   = Math.PI / 180.0;
	private static final double fromRad = 180.0 / Math.PI ;

	public static float fromRad(float radians) {
		return (float)(radians * fromRad ) % 360;
	}

	public static float toRad(float angle) {
		return (float)(angle * toRad);
	}

}
