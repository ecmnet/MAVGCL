/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.log.ulog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.comino.flight.model.service.AnalysisModelService;
import com.comino.mavcom.model.DataModel;

import me.drton.jmavlib.log.ulog.ULogReader;

public class UlogTest {

	public static void main(String[] args) {
		ULogReader reader = null;
		AnalysisModelService modelService = new AnalysisModelService(new DataModel());

		File file = new File("/Users/ecmnet/Desktop/test.ulg");
		try {
		  reader = new ULogReader(file.getAbsolutePath());
		  reader.getVersion().forEach((i,c) -> {
			  System.out.println(i+":"+c.toString());
		  });


		 List<String> list = new ArrayList<String>();
		 reader.getFields().forEach((i,p) -> {
				list.add(i);
			});
		Collections.sort(list);


		list.forEach(s -> {
			System.out.println(s);
		});

		//System.out.println(list.size()+" keyfigures found in log");

		  UlogtoModelConverter converter = new UlogtoModelConverter(reader,modelService.getModelList());
		  converter.doConversion();

		  reader.close();

//		  System.out.println(modelService.getModelList().size());
//
//		  modelService.getModelList().forEach(m -> {
//			System.out.println(m.getValue("ROLL"));
//		  });

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
