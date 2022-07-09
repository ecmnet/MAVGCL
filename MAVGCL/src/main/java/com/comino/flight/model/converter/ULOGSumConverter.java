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

import java.util.Map;

public class ULOGSumConverter extends SourceConverter {

	private String[] ulogKeyFigures = null;


	@Override
	public void setParameter(String kfname, String[] params) {
		this.ulogKeyFigures = params;

	}

	public ULOGSumConverter() {
		super();
	}

	@Override
	public double convert(Map<String,Object> ulogdata) {

		Object o1;
		double sum = 0;

		try {

			for(int i=0; i<ulogKeyFigures.length;i++) {

				o1 = ulogdata.get(ulogKeyFigures[i]);

				if(o1!=null) {
					if(o1 instanceof Long)
						sum += (double)(Long)o1 / 1000.0d;
					if(o1 instanceof Integer)
						sum +=  (double)(Integer)o1;
					else if(o1 instanceof Double)
						sum +=  ((Double)o1).doubleValue();
					else if(o1 instanceof Float)
						sum +=  ((Float)o1).doubleValue();
				}

			}

			return sum;

		} catch( Exception e) {
			return 0;
		}
	}

	@Override
	public String toString() {
		return "SUM";
	}

}
