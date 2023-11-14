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

	private Map<String, List<String>>   enum_map = new HashMap<String, List<String>>( );
	private Map<Integer, EventMetaData> event_map = new HashMap<Integer,EventMetaData>( );
	
	public static MAVEventMataData getInstance( ) {
		if(instance==null)
			instance = new MAVEventMataData("all_events.json");
		return instance;
	}

	private MAVEventMataData(String filename ) {
		
		System.out.println("Event parsing: "+filename);
	
		JsonElement root = JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream(filename)));

		// Get enums from defintion
		JsonElement enums = root.getAsJsonObject().get("components").getAsJsonObject().get("1").getAsJsonObject().get("enums");
		enums.getAsJsonObject().entrySet().forEach((enum_entry) -> {
			final List<String> entries = new ArrayList<>();
			enum_entry.getValue().getAsJsonObject().get("entries").getAsJsonObject().entrySet().forEach((enum_value)-> {
				entries.add(enum_value.getValue().getAsJsonObject().get("description").getAsString());
			});
			enum_map.put(enum_entry.getKey(), entries);
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
					Argument arg = new Argument();
					arg.name = argument.getAsJsonObject().get("name").getAsString();
					arg.type = argument.getAsJsonObject().get("type").getAsString();
					event_meta_data.arguments.add(arg);
				});
				}
				event_map.put(Integer.parseInt(event.getKey()),event_meta_data);
			});
		});
		
		System.out.println("Event metadata parsed");
		
//		event_map.forEach((id,event)->{
//			System.out.println(id+": "+event.message);
//			event.arguments.forEach((arg)-> {
//				System.out.println(" -> "+arg.name+": "+arg.type);
//			});
//		});
		
//		enum_map.forEach((k,v)-> {
//			System.out.println(k);
//			v.forEach((e)-> {
//				System.out.println(" --> "+e);
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
		public List<Argument> arguments = new ArrayList<Argument>();;
		
		public String buildMessage(msg_event event_msg) {
			String msg = this.message;
			
			for(int i=0; i<arguments.size();i++) {
//				// TODO Namespace in types (Offset 5=PX4::)
//				if(enum_map.containsKey(arguments.get(i).type.substring(5)))
//					msg = msg.replace("{"+(i+1)+"}", enum_map.get(arguments.get(i).type.substring(5)).get(event_msg.arguments[i]));
//				else	
				// TODO: Different types of arguments
				msg = msg.replace("{"+(i+1)+"}", "");
			}
			return msg;
		}
	}
	
	private class Argument {
		public String name;
		public String type;
	}

	




}
