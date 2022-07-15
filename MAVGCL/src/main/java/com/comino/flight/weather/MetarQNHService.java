package com.comino.flight.weather;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.mavlink.messages.MAV_SEVERITY;
import org.w3c.dom.Document;

import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.param.PX4Parameters;
import com.comino.mavutils.workqueue.WorkQueue;

public class MetarQNHService {
	
	// NOTE ICAO for Bavaria: LOWS

	private static final String service_url = "https://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=xml&stationString=%&hoursBeforeNow=3&mostRecent=true";
	private Document dom;

	private final WorkQueue wq = WorkQueue.getInstance();
	private String icao;
	private PX4Parameters params;
	private MSPLogger logger;


	public MetarQNHService(String icao, PX4Parameters params) {
		this.icao = icao;
		this.params = params;
		this.logger = MSPLogger.getInstance();
	}

	public void updateQNH() {

		new Thread(() -> {

			URLConnection conn = null;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {

				DocumentBuilder db = dbf.newDocumentBuilder();
				URL url = new URL(service_url.replace("%", icao));
				conn = url.openConnection();
				conn.setReadTimeout(100);
				conn.connect();
				dom = db.parse(new BufferedInputStream(conn.getInputStream()));
				String metar_data = dom.getElementsByTagName("raw_text").item(0).getTextContent();
				System.out.println("METAR data for used: "+metar_data);
				int i = metar_data.indexOf('Q')+1;
				float qnh = Float.parseFloat(metar_data.substring(i,i+4));	
				if(qnh>0 && params.getParam("SENS_BARO_QNH").value!=qnh) {
					System.out.println("QNH Update");
					logger.writeLocalMsg("QNH updated with "+qnh+", requires reboot.",MAV_SEVERITY.MAV_SEVERITY_WARNING);
					params.sendParameter("SENS_BARO_QNH",qnh);
				}

			} catch (Exception e) {
				logger.writeLocalMsg("QNH service not available.",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				System.out.println(e.getLocalizedMessage());
			}
		}).start();
	}

}
