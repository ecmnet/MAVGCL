package com.comino.flight.model.converter;

public class BitMaskConverter extends SourceConverter {

	@Override
	public float convert(float val) {
		if(((int)val & 1)==1)
			return 1;
		return 0;
	}

}
