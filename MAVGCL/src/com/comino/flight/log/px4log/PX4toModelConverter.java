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

package com.comino.flight.log.px4log;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.msp.model.DataModel;

import me.drton.jmavlib.log.BinaryLogReader;
import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.px4.PX4LogReader;

public class PX4toModelConverter {

	private BinaryLogReader reader;
	private List<AnalysisDataModel> list;

	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();


	public PX4toModelConverter(BinaryLogReader reader, List<AnalysisDataModel> list) {
		this.reader = reader;
		this.list = list;
	}


	public void doConversion() throws FormatErrorException {

		long tms_slot = 0; long tms = 0; boolean errorFlag = false;

		Map<String,Object> data = new HashMap<String,Object>();

		list.clear();
		AnalysisDataModel model = new AnalysisDataModel();

		try {

			while(tms < reader.getSizeMicroseconds()) {
				tms = reader.readUpdate(data)-reader.getStartMicroseconds();
				if(tms > tms_slot) {
					model.tms = tms;
					tms_slot += 50000;
					model.setValuesPX4(data, meta);
					list.add(model.clone());
				}
			}
			System.out.println(list.size()+" entries read. Timespan is "+tms_slot/1e6f+" sec");

		} catch(IOException e) {
			if(errorFlag)
				System.out.println("WARNING: Some of the key-figures were not available in the PX4Log");
			System.out.println(list.size()+" entries read. Timespan is "+tms_slot/1e6f+" sec");

		}
	}



}
