package com.comino.flight.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AnalysisDataModelMetaData {

	private static AnalysisDataModelMetaData instance = null;

	private Map<Integer,KeyFigureMetaData>        meta   = null;
	private Map<String,List<KeyFigureMetaData>> groups   = null;

	private int count = 0;

	public static AnalysisDataModelMetaData getInstance() {
		if(instance==null)
			instance = new AnalysisDataModelMetaData();
		return instance;
	}

	private AnalysisDataModelMetaData() {
		this.meta    = new HashMap<Integer,KeyFigureMetaData>();
		this.groups  = new HashMap<String,List<KeyFigureMetaData>>();

		try {
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = dBuilder.parse(getClass().getResourceAsStream("AnalysisDataModelMetaData.xml"));

			if (doc.hasChildNodes()) {
				String version = doc.getElementsByTagName("AnalysisDataModel")
						.item(0).getAttributes().getNamedItem("version").getTextContent();
				System.out.println("KeyFigureMetaData Version "+version);

				buildKeyFigureList(doc.getElementsByTagName("KeyFigure"));
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public Map<Integer,KeyFigureMetaData> getKeyFigureMap() {
		return meta;
	}

	public Map<String,List<KeyFigureMetaData>> getGroupMap() {
		return groups;
	}


	public void add(KeyFigureMetaData m) {
		this.meta.put(m.hash, m);
	}

	public KeyFigureMetaData getMetaData(String kf) {
		return meta.get(kf.toLowerCase().hashCode());
	}

	public List<KeyFigureMetaData> getKeyFigures() {
		List<KeyFigureMetaData> list = new ArrayList<KeyFigureMetaData>();
		meta.forEach((i,p) -> {
			list.add(p);
		});
		list.sort((KeyFigureMetaData o1, KeyFigureMetaData o2)->o1.desc1.compareTo(o2.desc1));
		return list;
	}

	public List<String> getGroups() {
		List<String> list = new ArrayList<String>();
		groups.forEach((i,p) -> {
			list.add(i);
		});
		Collections.sort(list);
		return list;
	}

	private void buildKeyFigureList(NodeList keyfigures) {
		for (count = 0; count < keyfigures.getLength(); count++) {
			KeyFigureMetaData keyfigure = buildKeyFigure(keyfigures.item(count));
			meta.put(keyfigure.hash,keyfigure);
		}
		System.out.println(count+" Keyfigures registered");
	}

	private KeyFigureMetaData buildKeyFigure(Node kf_node) {
		KeyFigureMetaData keyfigure = new KeyFigureMetaData(
				kf_node.getAttributes().getNamedItem("key" ).getTextContent(),
				kf_node.getAttributes().getNamedItem("desc").getTextContent(),
				kf_node.getAttributes().getNamedItem("uom" ).getTextContent(),
				kf_node.getAttributes().getNamedItem("mask").getTextContent());

		for(int i=0;i<kf_node.getChildNodes().getLength();i++) {
			Node node = kf_node.getChildNodes().item(i);
			if(node.getNodeName().equals("MSPSource")) {
				keyfigure.setMSPSource(node.getAttributes().getNamedItem("class").getTextContent(),
						node.getAttributes().getNamedItem("field").getTextContent());
			}
			if(node.getNodeName().equals("PX4Source")) {
				keyfigure.setPX4Source(node.getAttributes().getNamedItem("field").getTextContent());
			}
			if(node.getNodeName().equals("Groups")) {
				for(int j=0;j<node.getChildNodes().getLength();j++) {
					Node gr_node = node.getChildNodes().item(j);
					if(gr_node.getNodeName().equals("Group")) {
						String groupname = gr_node.getTextContent();
						List<KeyFigureMetaData> group = groups.get(groupname);
						if(group==null) {
							group = new ArrayList<KeyFigureMetaData>();
							groups.put(groupname, group);
						}
						group.add(keyfigure);
					}
				}
			}
		}
		return keyfigure;
	}
}
