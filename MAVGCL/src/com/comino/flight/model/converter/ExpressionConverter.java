package com.comino.flight.model.converter;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class ExpressionConverter extends SourceConverter {

	private Expression calc = null;

	@Override
	public void setParameter(String[] params) {
		Runnable r = new Runnable() {
			public void run() {
				calc = new ExpressionBuilder(params[0]).variable("val").build();
			}
		};
		new Thread(r).start();
	}

	@Override
	public float convert(float val) {
		calc.setVariable("val", val);
		return (float)calc.evaluate();
	}

	public ExpressionConverter() {
		super();
	}

}
