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

package com.comino.flight.tabs.parameters;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ParameterFactMetaData {

	private Map<String,ParameterAttributes> parameterList = null;

	public ParameterFactMetaData(String filename) {

		parameterList = new HashMap<String,ParameterAttributes>();

		try {
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = dBuilder.parse(getClass().getResourceAsStream(filename));

			if (doc.hasChildNodes()) {
				String v_major = doc.getElementsByTagName("parameter_version_major").item(0).getTextContent();
				String v_minor = doc.getElementsByTagName("parameter_version_minor").item(0).getTextContent();

				System.out.println("ParameterFactMetaData Version: "+v_major+"."+v_minor);

				buildParameterList(doc.getElementsByTagName("group"));
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	private void buildParameterList(NodeList groups) {
		for (int i = 0; i < groups.getLength(); i++) {
			Node group = groups.item(i);
			for(int g = 0;g<group.getChildNodes().getLength();g++) {
				addParameterToList(group.getAttributes().getNamedItem("name").getNodeValue(),group.getChildNodes().item(g));
			}
		}
	}

	private void addParameterToList(String group_name, Node parameter) {
		ParameterAttributes attributes = new ParameterAttributes(group_name);
		NamedNodeMap name_n = parameter.getAttributes();
		if(name_n==null)
			return;

		attributes.name = name_n.getNamedItem("name").getNodeValue().toUpperCase();

		NamedNodeMap type_n = parameter.getAttributes();
		if(type_n!=null)
			attributes.type  = type_n.getNamedItem("type").getNodeValue();

		NamedNodeMap default_n = parameter.getAttributes();
		if(default_n!=null)
			attributes.default_val  = Float.parseFloat(default_n.getNamedItem("default").getNodeValue());

		for(int i=0;i<parameter.getChildNodes().getLength();i++) {
			Node node = parameter.getChildNodes().item(i);
			if(node.getNodeName().equals("short_desc"))
				attributes.description = node.getTextContent();
			if(node.getNodeName().equals("long_desc"))
				attributes.description_long = node.getTextContent();
			if(node.getNodeName().equals("unit"))
				attributes.unit = node.getTextContent();
			if(node.getNodeName().equals("decimal"))
				attributes.decimals = Integer.parseInt(node.getTextContent());
			if(node.getNodeName().equals("min"))
				attributes.min_val = Float.parseFloat(node.getTextContent());
			if(node.getNodeName().equals("max"))
				attributes.max_val = Float.parseFloat(node.getTextContent());
		}

		parameterList.put(attributes.name, attributes);

	}

	public ParameterAttributes getMetaData(String parameterName) {
		return parameterList.get(parameterName.toUpperCase());
	}

}
