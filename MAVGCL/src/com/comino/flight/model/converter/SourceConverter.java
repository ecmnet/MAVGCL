package com.comino.flight.model.converter;

public abstract class SourceConverter {

	protected float[] params = null;

	public SourceConverter() {

	}

	public void setParameter(float[] params) {
		this.params = params;
	}

	public abstract float convert(float val);

}
