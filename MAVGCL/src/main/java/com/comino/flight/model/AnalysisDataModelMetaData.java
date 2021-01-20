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

package com.comino.flight.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AnalysisDataModelMetaData extends Observable {

	private static AnalysisDataModelMetaData instance = null;

	private Map<Integer,KeyFigureMetaData>        meta   = null;
	private Map<Integer,KeyFigureMetaData>        virt   = null;
	private Map<String,List<KeyFigureMetaData>> groups   = null;
	private List<KeyFigureMetaData>     sortedMetaList   = null;

	private int count = 0;
	private String version = "0.0";
	private String description = "not provided";

	public static AnalysisDataModelMetaData getInstance() {
		if(instance==null)
			instance = new AnalysisDataModelMetaData();
		return instance;
	}

	private AnalysisDataModelMetaData() {
		this.meta    = new HashMap<Integer,KeyFigureMetaData>(500);
		this.virt    = new HashMap<Integer,KeyFigureMetaData>(500);
		this.groups  = new HashMap<String,List<KeyFigureMetaData>>(500);

		loadModelMetaData(null);
	}

	public void loadModelMetaData(InputStream stream) {

			InputStream is = stream;
			meta.clear(); groups.clear(); virt.clear();

			if(is!=null) {
				buildDocument(is);
			} else {
				is = AnalysisDataModelMetaData.class.getResourceAsStream("AnalysisDataModelMetaData.xml");
				if(is==null) {
					System.err.println("Path to resources cannot be found: AnalysisDataModelMetaData.xml.");
					System.exit(0);
				}
				buildDocument(is);
			}
			sortedMetaList = buildSortedList();
			setChanged(); notifyObservers(null);

	}



	@Override
	public synchronized void addObserver(Observer o) {
		super.addObserver(o);
		setChanged();
		notifyObservers(null);
	}

	public String getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	}

	public Map<Integer,KeyFigureMetaData> getKeyFigureMap() {
		return meta;
	}

	public Map<Integer,KeyFigureMetaData> getVirtualKeyFigureMap() {
		return virt;
	}

	public Map<String,List<KeyFigureMetaData>> getGroupMap() {
		return groups;
	}


	public void add(KeyFigureMetaData m) {
		this.meta.put(m.hash, m);
	}

	public KeyFigureMetaData getMetaData(String kf) {
		if(kf!=null)
			return meta.get(kf.toLowerCase().hashCode());
		return null;
	}

	public List<KeyFigureMetaData> getKeyFigures() {
		return sortedMetaList;
	}

	public List<String> getGroups() {
		List<String> list = new ArrayList<String>(groups.keySet());
		Collections.sort(list);
		return list;
	}

	public void dump() {
		for(KeyFigureMetaData m : sortedMetaList) {
			System.out.println(m.toStringAll());
		}
	}

	private void buildDocument(InputStream is) {
		try {
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = dBuilder.parse(is);

			if (doc.hasChildNodes()) {
				version = doc.getElementsByTagName("AnalysisDataModel")
						.item(0).getAttributes().getNamedItem("version").getTextContent();
				description = doc.getElementsByTagName("AnalysisDataModel")
						.item(0).getAttributes().getNamedItem("description").getTextContent();

				buildKeyFigureList(doc.getElementsByTagName("KeyFigure"));

				sortGroupMaps();

				notifyObservers(is);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sortGroupMaps() {
		Thread t = new Thread(()-> {
			groups.forEach((g,l) -> {
				l.sort((KeyFigureMetaData o1, KeyFigureMetaData o2)->o1.desc1.compareTo(o2.desc1));
			});
		});
		t.setName("KeyFigure sorter worker");
		t.start();
	}

	private List<KeyFigureMetaData> buildSortedList() {

		List<KeyFigureMetaData> list = new ArrayList<KeyFigureMetaData>();
		meta.forEach((i,p) -> {
			list.add(p);
		});
		list.sort((KeyFigureMetaData o1, KeyFigureMetaData o2)->o1.desc1.compareTo(o2.desc1));
		return list;
	}

	private void buildKeyFigureList(NodeList keyfigures) {
		for (count = 0; count < keyfigures.getLength(); count++) {
			KeyFigureMetaData keyfigure = buildKeyFigure(keyfigures.item(count));
			meta.put(keyfigure.hash,keyfigure);
			if(keyfigure.isVirtual)
				virt.put(keyfigure.hash,keyfigure);
		}
		System.out.println(description+" (version "+version+") with "+count+" keyfigures ");
	}

	private KeyFigureMetaData buildKeyFigure(Node kf_node) {
		KeyFigureMetaData keyfigure = new KeyFigureMetaData(
				kf_node.getAttributes().getNamedItem("key" ).getTextContent(),
				kf_node.getAttributes().getNamedItem("desc").getTextContent(),
				kf_node.getAttributes().getNamedItem("uom" ).getTextContent(),
				kf_node.getAttributes().getNamedItem("mask").getTextContent());

		for(int i=0;i<kf_node.getChildNodes().getLength();i++) {
			Node node = kf_node.getChildNodes().item(i);
			if(node.getNodeName().equalsIgnoreCase("MSPSource")) {
				buildDataSource(KeyFigureMetaData.MSP_SOURCE, keyfigure,node);
			}
//			if(node.getNodeName().equalsIgnoreCase("PX4Source")) {
//				buildDataSource(KeyFigureMetaData.PX4_SOURCE, keyfigure,node);
//			}

			if(node.getNodeName().equalsIgnoreCase("ULogSource")) {
				buildDataSource(KeyFigureMetaData.ULG_SOURCE, keyfigure,node);
			}

//			if(node.getNodeName().equalsIgnoreCase("MAVLinkSource")) {
//				buildDataSource(KeyFigureMetaData.MAV_SOURCE, keyfigure,node);
//			}

			if(node.getNodeName().equalsIgnoreCase("VirtualSource")) {
				buildDataSource(KeyFigureMetaData.VIR_SOURCE, keyfigure,node);
			}

			if(node.getNodeName().equalsIgnoreCase("Validity")) {
				keyfigure.setBounds(
						Float.parseFloat(node.getAttributes().getNamedItem("min").getTextContent()),
						Float.parseFloat(node.getAttributes().getNamedItem("max").getTextContent())
						);

			}


			if(node.getNodeName().equalsIgnoreCase("Clip")) {
				keyfigure.setClipping(
						Float.parseFloat(node.getAttributes().getNamedItem("min").getTextContent()),
						Float.parseFloat(node.getAttributes().getNamedItem("max").getTextContent())
						);

			}

			if(node.getNodeName().equalsIgnoreCase("Groups")) {
				for(int j=0;j<node.getChildNodes().getLength();j++) {
					Node gr_node = node.getChildNodes().item(j);
					if(gr_node.getNodeName().equalsIgnoreCase("Group")) {
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

	private void buildDataSource(int type,KeyFigureMetaData keyfigure, Node node) {
		String[] params = null; String class_c = null;

		for(int j=0;j<node.getChildNodes().getLength();j++) {
			Node cnode = node.getChildNodes().item(j);
			if(cnode.getNodeName().equalsIgnoreCase("Converter")) {
				NamedNodeMap att = cnode.getAttributes();
				if(att.getLength()>1) {
					params = new String[att.getLength()-1];
					for(int p=1; p<att.getLength();p++)
						params[p-1] = att.item(p).getTextContent();
					class_c = att.getNamedItem("class").getTextContent();
				}
			}
		}
		keyfigure.setSource(type,
				node.getAttributes().getNamedItem("class")!=null ? node.getAttributes().getNamedItem("class").getTextContent() : null,
						node.getAttributes().getNamedItem("field")!=null ? node.getAttributes().getNamedItem("field").getTextContent() : null,
								class_c, params);
	}

}
