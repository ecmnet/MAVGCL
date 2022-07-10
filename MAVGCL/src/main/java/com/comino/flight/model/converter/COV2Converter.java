package com.comino.flight.model.converter;

import java.util.List;

import org.apache.commons.math3.stat.correlation.Covariance;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;

public class COV2Converter extends SourceConverter {
	
	private Covariance cov;
	
	private String kf1 = null;
	private String kf2 = null;
	private int    len = 0;

	private Double scale;
	
	public COV2Converter() {
		super();
		this.cov = new Covariance();
	}

	
	@Override
	public void setParameter(String kfname, String[] params) {
		this.kf1   = params[0];
		this.kf2   = params[1];
		this.len   = Integer.parseInt(params[2]);
		this.scale = Double.valueOf(params[3]);
		
	}
	
	@Override
	public double convert(AnalysisDataModel data) {
		
		List<AnalysisDataModel> list = AnalysisModelService.getInstance().getModelList();
		int index = AnalysisModelService.getInstance().getModelList().size()-1;
	

		if(index<len)
			return 0;
		
		double[] v1 = new double[len];
		double[] v2 = new double[len];
		
		int c=0;
		for(int i=index-len;i<index;i++) {
			v1[c] = list.get(i).getValue(kf1);
			v2[c] = list.get(i).getValue(kf2);	
			c++;
		}
		
		return cov.covariance(v1, v2, true) * scale;
		
	}
	
	@Override
	public String toString() {
		return "COV";
	}

}
