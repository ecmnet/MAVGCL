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

package com.comino.flight.model;

import com.comino.mavcom.model.DataModel;

public class AnalysisModelTest {

	public static void main(String[] args) {

		 DataModel m = new DataModel();
		 m.hud.ag = 7.2f;
		 m.state.l_y = 3.99f;
		 m.state.l_z = 0.12f;
		 m.target_state.l_z = 0.15f;

		 AnalysisDataModel model = new AnalysisDataModel();

		 AnalysisDataModelMetaData md = AnalysisDataModelMetaData.getInstance();

		 System.out.println(md.getMetaData("ALTGL").toString());


         for(int i=0;i<1000;i++)
		     model.setValues(KeyFigureMetaData.MSP_SOURCE,m, md);


		 AnalysisDataModel model2 = (AnalysisDataModel)model.clone();
		 System.out.println(model2.getValue("ALTGL"));
		 System.out.println(model2.getValue("LPOSY"));
		 System.out.println(model2.getValue("LPOSZ"));
		 System.out.println(model2.getValue("SPLPOSZ"));

		 md.getKeyFigureMap().forEach((i,e) -> {
			System.out.println( e.desc2 );
		 });


		 md.getGroupMap().forEach((g,p) -> {
			 System.out.println(g);
			 p.forEach(k -> {
				 System.out.println("---> "+k.desc1);
			 });
		 });

	}

}
