package com.comino.flight.model.converter;

public class BitMaskConverter extends SourceConverter {

	int mask = 0;

	@Override
	public void setParameter(String[] params) {
		this.mask = Integer.parseInt(params[0]);
	}


	@Override
	public float convert(float val) {
		if(((int)val >> mask & 1) ==1 )
			return 1;
		return 0;
	}

	public BitMaskConverter() {
		super();
	}

}
