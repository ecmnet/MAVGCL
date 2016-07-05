package com.comino.flight.model;

import com.comino.msp.model.DataModel;

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
		     model.setValues(m, md);


		 AnalysisDataModel model2 = model.clone();
		 System.out.println(model2.getValue("ALTGL"));
		 System.out.println(model2.getValue("LPOSY"));
		 System.out.println(model2.getValue("LPOSZ"));
		 System.out.println(model2.getValue("SPLPOSZ"));

		 md.getKeyFigures().forEach((i,e) -> {
			System.out.println( e.desc2 );
		 });


		 md.getGroups().forEach((g,p) -> {
			 System.out.println(g);
			 p.forEach(k -> {
				 System.out.println("---> "+k.desc1);
			 });
		 });

	}

}
