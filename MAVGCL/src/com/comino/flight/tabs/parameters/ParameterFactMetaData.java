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
			if (doc.hasChildNodes())
				buildParameterList(doc.getElementsByTagName("group"));
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
			if(node.getNodeName().equals("min_val"))
				attributes.min_val = Float.parseFloat(node.getTextContent());
			if(node.getNodeName().equals("max_val"))
				attributes.min_val = Float.parseFloat(node.getTextContent());
		}

		parameterList.put(attributes.name, attributes);

	}

	public ParameterAttributes getMetaData(String parameterName) {
		return parameterList.get(parameterName.toUpperCase());
	}

}
