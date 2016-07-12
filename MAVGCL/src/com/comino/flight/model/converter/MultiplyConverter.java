package com.comino.flight.model.converter;

public class MultiplyConverter extends SourceConverter {

	@Override
	public float convert(float val) {
		return val * params[0];
	}

	public MultiplyConverter() {
		super();
	}

}
