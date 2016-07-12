package com.comino.flight.model.converter;

import java.util.List;

public abstract class SourceConverter {

	protected List<Float> params = null;

	public void setParameter(List<Float> params) {
		this.params = params;
	}

	public abstract float convert(float val);

}
