/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.px4log;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.model.types.MSTYPE;
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
								//	System.err.println(px4Name+" not found in file");
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
