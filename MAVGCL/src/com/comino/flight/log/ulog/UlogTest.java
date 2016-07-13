package com.comino.flight.log.ulog;

import java.io.File;

import com.comino.flight.log.px4log.PX4toModelConverter;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.msp.model.DataModel;

import me.drton.jmavlib.log.ulog.ULogReader;

public class UlogTest {

	public static void main(String[] args) {
		ULogReader reader = null;
		AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();
		AnalysisModelService modelService = AnalysisModelService.getInstance(new DataModel());

		File file = new File("/Users/ecmnet/Desktop/test.ulg");
		try {
		  reader = new ULogReader(file.getAbsolutePath());
		  reader.getVersion().forEach((i,c) -> {
			  System.out.println(i+":"+c.toString());
		  });

//		 System.out.println("Fields: "+reader.getFields().size());
//		 reader.getFields().forEach( (i,s)-> {
//			 System.out.println(i+":"+s);
//		 });

		  UlogtoModelConverter converter = new UlogtoModelConverter(reader,modelService.getModelList());
		  converter.doConversion();

		  reader.close();

		  System.out.println(modelService.getModelList().size());

		  modelService.getModelList().forEach(m -> {
			System.out.println(m.getValue("ROLL"));
		  });

//		  if(reader.getErrors().size()>0)
//				for(Exception k : reader.getErrors())
//					System.err.println(k.getMessage());


		} catch(Exception e) {
//			if(reader.getErrors().size()>0)
//				for(Exception k : reader.getErrors())
//					System.err.println(k.getMessage());

			e.printStackTrace();
		}

	}

}
