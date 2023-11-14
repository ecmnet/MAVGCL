package com.comino.flight.events;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mavlink.messages.lquac.msg_event;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;



public class MAVEventMataData {


	private static MAVEventMataData instance;

	private Map<String, Map<Integer,String>>   enum_map = new HashMap<String, Map<Integer,String>>( );
	private Map<Integer, EventMetaData> event_map = new HashMap<Integer,EventMetaData>( );
	private String namespace;

	public static MAVEventMataData getInstance( ) {
		if(instance==null)
			instance = new MAVEventMataData("all_events.json");
		return instance;
	}

	private MAVEventMataData(String filename ) {

		JsonElement root = JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream(filename)));

		JsonElement name_space = root.getAsJsonObject().get("components").getAsJsonObject().get("1").getAsJsonObject().get("namespace");
		namespace = name_space.getAsString();

		// Get enums from defintion
		JsonElement enums = root.getAsJsonObject().get("components").getAsJsonObject().get("1").getAsJsonObject().get("enums");
		enums.getAsJsonObject().entrySet().forEach((enum_entry) -> {
			final HashMap<Integer,String> entries = new HashMap<>();
			enum_entry.getValue().getAsJsonObject().get("entries").getAsJsonObject().entrySet().forEach((enum_value)-> {
				entries.put(Integer.parseInt(enum_value.getKey()),enum_value.getValue().getAsJsonObject().get("description").getAsString());
			});
			enum_map.put(namespace+"::"+enum_entry.getKey(), entries);
		});

		// Get events from definition
		JsonElement event_groups = root.getAsJsonObject().get("components").getAsJsonObject().get("1").getAsJsonObject().get("event_groups");
		event_groups.getAsJsonObject().entrySet().forEach((map) -> {
			map.getValue().getAsJsonObject().get("events").getAsJsonObject().entrySet().forEach((event) -> {
				EventMetaData event_meta_data = new EventMetaData( );
				event_meta_data.message = event.getValue().getAsJsonObject().get("message").getAsString();
				var _arguments = event.getValue().getAsJsonObject().get("arguments");
				if(_arguments!=null) {
					_arguments.getAsJsonArray().forEach((argument) -> {
						String type = argument.getAsJsonObject().get("type").getAsString();
						if(type!=null && type.length()>0)
						  event_meta_data.arguments.add(type);
					});
				}
				event_map.put(Integer.parseInt(event.getKey()),event_meta_data);
			});
		});


		System.out.println("Event metadata parsed for namespace: "+namespace);

		//		event_map.forEach((id,event)->{
		//			System.out.println(id+": "+event.message);
		//			event.arguments.forEach((arg)-> {
		//				System.out.println(" -> "+arg.name+": "+arg.type);
		//			});
		//		});

		//		enum_map.forEach((k,v)-> {
		//			System.out.println(k);
		//			v.forEach((e,l)-> {
		//				System.out.println(" --> "+e+":"+l);
		//			});
		//		});

	}

	public String buildMessageFromMAVLink( msg_event event_msg) {
		EventMetaData emd = this.event_map.get((int)(event_msg.id & 0x0FFFFFF));
		if(emd == null)
			return "[px4] Event "+(int)(event_msg.id & 0x0FFFFFF)+" received";
		return "[px4] "+emd.buildMessage(event_msg);
	}

	private class EventMetaData {

		public String message;
		public List<String> arguments = new ArrayList<String>();;

		public String buildMessage(msg_event event_msg) {
			String msg = this.message;
			for(int i=0; i<arguments.size();i++) {
				// Complex types
				if(enum_map.containsKey(arguments.get(i)))
					msg = msg.replace("{"+(i+1)+"}", enum_map.get(arguments.get(i)).get(event_msg.arguments[i]));
				else	{
				// Simple types
					switch(arguments.get(i)) {
					case "uint8_t":
						msg = msg.replace("{"+(i+1)+"}", String.valueOf(event_msg.arguments[i] & 0xFF));
						break;
					case "uint16_t":
						msg = msg.replace("{"+(i+1)+"}", String.valueOf(event_msg.arguments[i] & 0xFFFF));
						break;
					case "float_t":
						msg = msg.substring(0,msg.indexOf('{'));
						break;
					default:
						msg = msg.substring(0,msg.indexOf('{'));
					}
				}
			}
			return msg;
		}
	}
}
