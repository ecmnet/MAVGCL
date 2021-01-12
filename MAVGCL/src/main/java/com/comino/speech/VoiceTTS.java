package com.comino.speech;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import com.comino.flight.file.FileHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javazoom.jl.player.Player;

public class VoiceTTS implements Runnable {


	private static final String request  = "http://translate.google.com.vn/translate_tts?e=UTF-8&&tl=en&client=tw-ob&q=";

	private String                      path = null;
	private String                      text = null;
	private ArrayList<VoiceCacheEntry> cache = null;
	private Thread                    thread = null;

	private static VoiceTTS             tts  = null;

	public static VoiceTTS getInstance() {
		if(tts==null)
			tts = new VoiceTTS();
		return tts;
	}

	public VoiceTTS()  {

		try {
			this.path = FileHandler.getInstance().getBasePath()+"/SpeechCache";
			FileReader f = new FileReader(path);
			Gson gson = new Gson();
			Type listType = new TypeToken<ArrayList<VoiceCacheEntry>>() { }.getType();
			cache = gson.fromJson(f, listType);
			System.out.println("Speech cache loaded with "+cache.size()+" entries");
		} catch (Exception e) {
			this.cache = new ArrayList<VoiceCacheEntry>();
		}
	}

	public void talk(String text) {
		this.text = text;
		if(thread!=null && thread.isAlive()) {
			  return;
		}
		thread = new Thread(this);
		thread.start();
		try { Thread.sleep(100);} catch (InterruptedException e) { }
	}


	public void run() {

		byte[] buffer = null;

		Iterator<VoiceCacheEntry> i = cache.iterator();
		while(i.hasNext() && buffer==null) {
			try {
				buffer = i.next().get(URLEncoder.encode(text, "utf-8").toLowerCase());
			} catch (UnsupportedEncodingException e) { }
		}

		if(buffer==null) {
			try {
				String t = URLEncoder.encode(text, "utf-8").toLowerCase();
				URL url = new URL(request+"'"+t+"'");
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				connection.setRequestMethod("GET");
				connection.setRequestProperty("user-agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) ");
				connection.setUseCaches(false);
				connection.connect();
				BufferedInputStream bufIn =
						new BufferedInputStream(connection.getInputStream());
				byte[] buf = new byte[100];
				int n;
				ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
				while ((n = bufIn.read(buf)) > 0) {
					bufOut.write(buf, 0, n);
				}

				buffer = bufOut.toByteArray();
				VoiceCacheEntry entry = new VoiceCacheEntry(text,buffer);
				cache.add(entry);
				bufOut.close();

				Gson gson = new Gson();
				String json = gson.toJson(cache);
				try {
					OutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
					stream.write(json.getBytes());
					stream.close();
				} catch (Exception e) {

				}

			} catch(Exception e) { System.err.println(e.getMessage()); e.printStackTrace(); }
		}

		try {

			Player player = new Player(new ByteArrayInputStream(buffer));
			player.play();
			player.close();


		} catch (Exception e) {

		}

	}


	public static void main(String[] args)  {
        VoiceTTS.getInstance().talk("Landed");
        VoiceTTS.getInstance().talk("Takeoff");

	}


}
