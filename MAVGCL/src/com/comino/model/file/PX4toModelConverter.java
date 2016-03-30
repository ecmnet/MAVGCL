package com.comino.model.file;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.msp.model.DataModel;

import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.px4.PX4LogReader;

public class PX4toModelConverter {

	private PX4LogReader reader;
	private List<DataModel> list;


	public PX4toModelConverter(PX4LogReader reader, List<DataModel> list) {
		this.reader = reader;
		this.list = list;
	}


	public void doConversion() throws FormatErrorException {

		long tms_slot = 0; long tms = 0;

		Map<String,Object> data = new HashMap<String,Object>();

		MSTYPE[] types = MSTYPE.values();
		for(int i=0; i< types.length;i++) {
			if(MSTYPE.getPX4LogName(types[i]).length()>0) {
				data.put(MSTYPE.getPX4LogName(types[i]), null);
			}
		}



		list.clear();
		DataModel model = new DataModel();

		Object val=null;

		try {

			while(tms < reader.getSizeMicroseconds()) {
				tms = reader.readUpdate(data)-reader.getStartMicroseconds();
				if(tms > tms_slot) {
					model.tms = tms;
					tms_slot += 50000;
					for(int i=0; i< types.length;i++) {
						if(MSTYPE.getPX4LogName(types[i]).length()>0) {
							String px4Name = MSTYPE.getPX4LogName(types[i]);

							try {
								val = data.get(px4Name);
								if(val == null) {
									System.err.println(px4Name+" not found in file");
									continue;
								}

								if(val instanceof Double) {
									MSTYPE.putValue(model, types[i], ((Double)val).floatValue());
								}
								else if(val instanceof Float) {
									MSTYPE.putValue(model, types[i], ((Float)val).floatValue());
								}
								else if(val instanceof Integer) {
									MSTYPE.putValue(model, types[i], ((Integer)val).floatValue());
								} else {
									MSTYPE.putValue(model, types[i], ((float)val));
								}

							} catch(Exception e) {
								   System.err.println(px4Name+":"+ val.getClass().getSimpleName()+" : "+e.getMessage());
							}
						}
					}
					list.add(model.clone());
				}
			}

		} catch(IOException e) {
			System.out.println(list.size()+" entries read. Timespan is "+tms_slot/1e6f+" sec");

		}
	}



}
