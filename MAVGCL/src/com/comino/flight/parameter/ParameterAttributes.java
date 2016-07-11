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

package com.comino.flight.parameter;

import java.util.HashMap;

public class ParameterAttributes  implements Comparable<ParameterAttributes> {

	public float   default_val = 0;
	public float   min_val   = -Float.MAX_VALUE;
	public float   max_val   =  Float.MAX_VALUE;
	public float   value     = 0;
	public float   increment = 0;
	public int     vtype     = 0;
	public int     decimals  = 3;
	public String  name = null;
	public String  type = null;
	public String  description = null;
	public String  description_long = null;
	public String  unit = "";
	public String  group_name = null;

	public boolean reboot_required = false;
	public HashMap<Integer,String> valueList = null;

	public ParameterAttributes(String group_name) {
		this.group_name = group_name;
		this.valueList = new HashMap<Integer,String>();
	}

	public ParameterAttributes(String name, String group_name) {
		this(group_name);
		this.name = name;
		this.type = "none";
		this.description = "none";
		this.description_long = "none";
	}


	public String toString() {
		return "group="+group_name+" name="+name+" description="+description+" type="+type;
	}

	@Override
	public int compareTo(ParameterAttributes o) {
       return this.name.compareTo(o.name);
	}
}
