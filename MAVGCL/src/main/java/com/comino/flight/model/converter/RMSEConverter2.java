/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.model.converter;

import java.util.List;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;

public class RMSEConverter2 extends SourceConverter {

	String kf_val = null;
	String kf_sp  = null;
	int    frame  = 0;
	String kf_name = null;
	

	@Override
	public void setParameter(String kfname, String[] params) {
		this.kf_name = kfname;
		this.kf_val = params[0];
		this.kf_sp  = params[1];
		this.frame  = Integer.parseInt(params[2]);
	}


	@Override
	public double convert(AnalysisDataModel data) {

		double rmse = 0; double kf = 0; double sp = 0;
		
		List<AnalysisDataModel> list = AnalysisModelService.getInstance().getModelList();

		if(list.size()<frame) {
			return 0;
		}

		if(list.get(list.size()-1).getValue(kf_name)!=0) {
			kf = data.getValue(kf_val);
			sp = data.getValue(kf_sp);
			if(Double.isNaN(kf) || Double.isNaN(sp))
				return 0;
			rmse = list.get(list.size()-1).getValue(kf_name);
			rmse = rmse * rmse * frame;
			rmse = rmse + ((kf - sp ) * (kf - sp ));
			kf = list.get(list.size()-frame).getValue(kf_val);
			sp = list.get(list.size()-frame).getValue(kf_sp);
			rmse = rmse - ((kf - sp ) * (kf - sp ));
			return Math.sqrt(rmse/frame);
		} else {

		for(int i=list.size()-frame;i<list.size();i++) {
			kf = list.get(i).getValue(kf_val);
			sp = list.get(i).getValue(kf_sp);
			if(Double.isNaN(kf) || Double.isNaN(sp))
				return 0;
			rmse = rmse + ((kf - sp ) * (kf - sp ));
		}
		return Math.sqrt(rmse/frame);
		}
	}

	public RMSEConverter2() {
		super();
	}


	@Override
	public String toString() {
		return "RMSE: "+ kf_val +" ("+frame+")";
	}



}
