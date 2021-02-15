package com.comino.flight.weather;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class MetarQNHService {

	private static final String service_url = "https://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=xml&stationString=%&hoursBeforeNow=3&mostRecent=true";
	private Document dom;
	private float    qnh = 0;

	public MetarQNHService(String icao) {

		URLConnection conn = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {

			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL(service_url.replace("%", icao));
			conn = url.openConnection();
			conn.setReadTimeout(200);
			conn.connect();
			dom = db.parse(new BufferedInputStream(conn.getInputStream()));
			String metar_data = dom.getElementsByTagName("raw_text").item(0).getTextContent();
			System.out.println("METAR data for used: "+metar_data);
			int i = metar_data.indexOf('Q')+1;
			qnh = Float.parseFloat(metar_data.substring(i,i+4));		

		} catch (Exception e) {
			System.err.println(e.getMessage());
			qnh = 0;
		}

	}

	public float getQNH() {
		return qnh;
	}


	public static void main(String[] args) {
		System.out.println(new MetarQNHService("EDDM").getQNH());

	}

}
