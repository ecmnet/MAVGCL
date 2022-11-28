package com.comino.flight.log.ulog.entry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ULogEntry {

	public long time_utc;
	public long size;
	public int  id;

	private String name_s;
	private String size_s;

	private static final DateFormat date_f = new EntryDateFormat();;

	public ULogEntry(int id, long time, long size) {

		this.time_utc = time;
		this.size = size;
		this.id = id;

		this.name_s = date_f.format(time*1000);

		float s = size / 1024f;
		if(s < 1024)
			this.size_s = String.format("%#.0fkb", s);
		else
			this.size_s = String.format("%#.1fMb", s/1024f);
	}

	public int getId() {
		return id;
	}
	public String getName() {
		return name_s;
	}
	public String getSize() {
		return size_s;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void setName(String name) {
		this.name_s = name;
	}
	public void setSize(String size) {
		this.size_s = size;
	}

}

final class EntryDateFormat extends SimpleDateFormat {

	private static final long serialVersionUID = 2351913991697815380L;

	public EntryDateFormat() {
		super("YYYY-MM-dd HH:mm:ss");
		super.setTimeZone(TimeZone.getDefault());
	}

}
