package com.comino.flight.model.converter;

public abstract class SourceConverter {


	public SourceConverter() {
	}

	public abstract void setParameter(String[] params);

	public abstract float convert(float val);

}
