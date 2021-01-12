/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.jmavlib.extensions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_logging_data;
import org.mavlink.messages.lquac.msg_logging_data_acked;

import com.comino.mavcom.log.MSPLogger;

import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.ulog.FieldFormat;
import me.drton.jmavlib.log.ulog.MessageAddLogged;
import me.drton.jmavlib.log.ulog.MessageData;
import me.drton.jmavlib.log.ulog.MessageDropout;
import me.drton.jmavlib.log.ulog.MessageFlagBits;
import me.drton.jmavlib.log.ulog.MessageFormat;
import me.drton.jmavlib.log.ulog.MessageInfo;
import me.drton.jmavlib.log.ulog.MessageInfoMultiple;
import me.drton.jmavlib.log.ulog.MessageLog;
import me.drton.jmavlib.log.ulog.MessageParameter;

public class UlogMAVLinkParser  {

	private static final byte MESSAGE_TYPE_FORMAT = (byte) 'F';
	private static final byte MESSAGE_TYPE_DATA = (byte) 'D';
	private static final byte MESSAGE_TYPE_INFO = (byte) 'I';
	private static final byte MESSAGE_TYPE_PARAMETER = (byte) 'P';
	private static final byte MESSAGE_TYPE_ADD_LOGGED_MSG = (byte) 'A';
	private static final byte MESSAGE_TYPE_INFO_MULTIPLE = (byte) 'M';
	private static final byte MESSAGE_TYPE_REMOVE_LOGGED_MSG = (byte) 'R';
	private static final byte MESSAGE_TYPE_SYNC = (byte) 'S';
	private static final byte MESSAGE_TYPE_DROPOUT = (byte) 'O';
	private static final byte MESSAGE_TYPE_LOG = (byte) 'L';
	private static final byte MESSAGE_TYPE_FLAG_BITS = (byte) 'B';

	private static final int INCOMPAT_FLAG0_DATA_APPENDED_MASK = 1<<0;

	private ByteBuffer buffer = null;
	private long logStartTimestamp;

	// Header maps
	private Map<String, MessageFormat> messageFormats = new HashMap<String, MessageFormat>();
	private Map<String, Object> parameters = new HashMap<String, Object>();
	private Map<String, List<ParamUpdate>> parameterUpdates = new HashMap<String, List<ParamUpdate>>();
	private List<Subscription> messageSubscriptions = new ArrayList<Subscription>();
	private Map<String, String> fieldsList = new HashMap<String, String>();

	// Data map

	private Map<String, Object> data = new HashMap<String, Object>();

	// System info
	private String systemName;
	private String hw_version;
	private String sw_version;
	private long utcTimeReference;

	// Helpers
	private boolean nestedParsingDone = false;

	private String hardfaultPlainText = "";

	private Vector<Long> appendedOffsets = new Vector<Long>();
	int currentAppendingOffsetIndex = 0; // current index to appendedOffsets for the next appended offset

	private long timeStart=-1;

	public UlogMAVLinkParser() {
		buffer = ByteBuffer.allocate(300000);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.clear();
	}

	public void addToBuffer(msg_logging_data msg, boolean ok) {
		try {
			if(ok) {
				for (int i = 0; i < msg.data.length; i++)
					buffer.put((byte)(msg.data[i] & 0x00FF));
			} else {
				buffer.clear();
				for (int i = msg.first_message_offset; i < msg.data.length; i++)
					buffer.put((byte)(msg.data[i] & 0x00FF));
			}
		} catch(Exception o) {
			          o.printStackTrace();
		}
	}

	public void addToBuffer(msg_logging_data_acked msg) {
		for (int i = 0; i < msg.data.length; i++)
			buffer.put((byte)(msg.data[i] & 0x00FF));
	}

	public Map<String, String> getFieldList() {
		return fieldsList;
	}

	public Map<String, Object> getDataBuffer() {
		return data;
	}

	public void clearBuffer() {
		buffer.clear();
	}

	public void reset() {
		messageFormats.clear();
		parameters.clear();
		parameterUpdates.clear();
		messageSubscriptions.clear();
		fieldsList.clear();
	//	data.clear();
		nestedParsingDone = false;
		buffer.clear();
	}

	public String getSystemInfo() {
		return "Sys:"+systemName+" HWVer:"+hw_version+" SWVer:"+sw_version+" UTCref:"+utcTimeReference;
	}

	public boolean checkHeader() {
		buffer.flip();
		if (!checkMagicHeader()) {
			buffer.compact();
			return false;
		}
		MSPLogger.getInstance().writeLocalMsg("[mgc] ULOG Logging started",
				MAV_SEVERITY.MAV_SEVERITY_DEBUG);
		buffer.compact();
		logStartTimestamp = 0;
		return true;
	}

	public void parseData(boolean debug) {
		Object msg = null;
		buffer.flip();
		while ((msg = readMessage()) != null) {
			if(debug)
				System.out.println(msg);
			if(msg instanceof MessageData) {
				if (timeStart < 0)
					timeStart = ((MessageData)msg).timestamp;
				applyMsg(data, (MessageData) msg);
			}
		}
		buffer.compact();
	}

	public void parseHeader()   {
		Object msg = null;  long lastTime = -1;
		buffer.flip();
		while ((msg = readMessage()) != null) {
			//System.err.println(msg);

			if (msg instanceof MessageFlagBits) {
				MessageFlagBits msgFlags = (MessageFlagBits) msg;
				// check flags
				if ((msgFlags.incompatFlags[0] & INCOMPAT_FLAG0_DATA_APPENDED_MASK) != 0) {
					for (int i = 0; i < msgFlags.appendedOffsets.length; ++i) {
						if (msgFlags.appendedOffsets[i] > 0) {
							appendedOffsets.add(msgFlags.appendedOffsets[i]);
						}
					}
					if (appendedOffsets.size() > 0) {
						System.out.println("log contains appended data");
					}
				}
				boolean containsUnknownIncompatBits = false;
				if ((msgFlags.incompatFlags[0] & ~0x1) != 0)
					containsUnknownIncompatBits = true;
				for (int i = 1; i < msgFlags.incompatFlags.length; ++i) {
					if (msgFlags.incompatFlags[i] != 0)
						containsUnknownIncompatBits = true;
				}
				if (containsUnknownIncompatBits) {
					System.err.println("Log contains unknown incompatible bits. Refusing to parse the log.");
				}

			} else if (msg instanceof MessageFormat) {
				MessageFormat msgFormat = (MessageFormat) msg;
				messageFormats.put(msgFormat.name, msgFormat);

			} else if (msg instanceof MessageInfoMultiple) {
				MessageInfoMultiple msgInfo = (MessageInfoMultiple) msg;
				//System.out.println(msgInfo.getKey());
				if ("hardfault_plain".equals(msgInfo.getKey())) {
					// append all hardfaults to one String (we should be looking at msgInfo.isContinued as well)
					hardfaultPlainText += (String)msgInfo.value;
				}


			} else if (msg instanceof MessageAddLogged) {

				//from now on we cannot have any new MessageFormat's, so we
				//can parse the nested types
				if (!nestedParsingDone) {
					for (MessageFormat m : messageFormats.values()) {
						m.parseNestedTypes(messageFormats);
					}
					//now do a 2. pass to remove the last padding field
					for (MessageFormat m : messageFormats.values()) {
						m.removeLastPaddingField();
					}
					nestedParsingDone = true;
				}
				MessageAddLogged msgAddLogged = (MessageAddLogged) msg;
				MessageFormat msgFormat = messageFormats.get(msgAddLogged.name);
				if(msgFormat == null) {
					System.err.println("Format of subscribed message not found: " + msgAddLogged.name);
					continue;
				}
				Subscription subscription = new Subscription(msgFormat, msgAddLogged.multiID);

				if (msgAddLogged.msgID < messageSubscriptions.size()) {
					messageSubscriptions.set(msgAddLogged.msgID, subscription);
				} else {
					while (msgAddLogged.msgID > messageSubscriptions.size())
						messageSubscriptions.add(null);
					messageSubscriptions.add(subscription);
				}
				if (msgAddLogged.multiID > msgFormat.maxMultiID)
					msgFormat.maxMultiID = msgAddLogged.multiID;


			} else if (msg instanceof MessageParameter) {
				MessageParameter msgParam = (MessageParameter) msg;
				lastTime = System.currentTimeMillis();
				if (parameters.containsKey(msgParam.getKey())) {
					MSPLogger.getInstance().writeLocalMsg("[mgc] Update to parameter: " + msgParam.getKey() +
							" value: " + msgParam.value + " at t = " + lastTime,MAV_SEVERITY.MAV_SEVERITY_DEBUG);
					// maintain a record of parameters which change during flight
					if (parameterUpdates.containsKey(msgParam.getKey())) {
						parameterUpdates.get(msgParam.getKey()).add(new ParamUpdate(msgParam.getKey(), msgParam.value, lastTime));
					} else {
						List<ParamUpdate> updateList = new ArrayList<ParamUpdate>();
						updateList.add(new ParamUpdate(msgParam.getKey(), msgParam.value, lastTime));
						parameterUpdates.put(msgParam.getKey(), updateList);
					}
				} else {
					// add parameter to the parameters Map
					parameters.put(msgParam.getKey(), msgParam.value);
				}

			} else if (msg instanceof MessageInfo) {
				MessageInfo msgInfo = (MessageInfo) msg;
				if ("sys_name".equals(msgInfo.getKey())) {
					systemName = (String) msgInfo.value;
				} else if ("ver_hw".equals(msgInfo.getKey())) {
					hw_version = (String) msgInfo.value;
				} else if ("ver_sw".equals(msgInfo.getKey())) {
					sw_version = (String) msgInfo.value;
				} else if ("time_ref_utc".equals(msgInfo.getKey())) {
					utcTimeReference = ((long) ((Number) msgInfo.value).intValue()) * 1000 * 1000;
				}

			}
			timeStart=-1;
		}

		buffer.compact();

	}


	public void buildSubscriptions() {
		for (int k = 0; k < messageSubscriptions.size(); ++k) {
			Subscription s = messageSubscriptions.get(k);
			if (s != null) {
				//	System.out.println(k+": "+s.format.name);
				MessageFormat msgFormat = s.format;
				if (msgFormat.name.charAt(0) != '_') {
					int maxInstance = msgFormat.maxMultiID;
					for (int i = 0; i < msgFormat.fields.size(); i++) {
						FieldFormat fieldDescr = msgFormat.fields.get(i);
						if (!fieldDescr.name.contains("_padding") && fieldDescr.name != "timestamp") {
							for (int mid = 0; mid <= maxInstance; mid++) {
								if (fieldDescr.isArray()) {
									for (int j = 0; j < fieldDescr.size; j++) {
										fieldsList.put(msgFormat.name + "_" + mid + "." + fieldDescr.name + "[" + j + "]", fieldDescr.type);
									}
								} else {
									fieldsList.put(msgFormat.name + "_" + mid + "." + fieldDescr.name, fieldDescr.type);
								}
							}
						}
					}
				}
			}
		}
	}

	public Object readMessage()  {

		if(buffer.remaining()<3)
			return null;

		int s1 = buffer.get() & 0x00FF;
		int s2 = buffer.get() & 0x00FF;
		int msgSize = s1 + (256 * s2);
		int msgType = buffer.get() & 0x00FF;

		// check if we cross an appending boundary: if so, we need to reset the position and skip this message
		if (currentAppendingOffsetIndex < appendedOffsets.size()) {
			if (buffer.position() + 3 + msgSize > appendedOffsets.get(currentAppendingOffsetIndex)) {
				//System.out.println("Jumping to next position: "+pos + ", next: "+appendedOffsets.get(currentAppendingOffsetIndex));
				buffer.position((appendedOffsets.get(currentAppendingOffsetIndex).intValue()));
				++currentAppendingOffsetIndex;
			}
		}

		try {

			if (msgSize > buffer.remaining()-3) {
				buffer.position(buffer.position()-3);
				return null;
			}

			switch (msgType) {

			case MESSAGE_TYPE_DATA:

				s1 = buffer.get() & 0x00FF;
				s2 = buffer.get() & 0x00FF;
				int msgID = s1 + (256 * s2);

				Subscription subscription = null;
				if (msgID < messageSubscriptions.size())
					subscription = messageSubscriptions.get(msgID);
				if (subscription == null) {
					// System.err.println("Unknown DATA subscription ID: " + msgID);
					buffer.position(buffer.position()+msgSize-5);
					return null;
				}
				try {
					return new MessageData(subscription.format, buffer, subscription.multiID);
				} catch (FormatErrorException e) {
				//	System.err.println(e.getMessage()+": " + msgID);
					buffer.position(buffer.position()+msgSize-5);
					return null;
				}
			case MESSAGE_TYPE_FLAG_BITS:
				return  new MessageFlagBits(buffer, msgSize);
			case MESSAGE_TYPE_INFO:
				return new MessageInfo(buffer);
			case MESSAGE_TYPE_INFO_MULTIPLE:
				return new MessageInfoMultiple(buffer);

			case MESSAGE_TYPE_PARAMETER:
				return new MessageParameter(buffer);
			case MESSAGE_TYPE_FORMAT:
				return new MessageFormat(buffer, msgSize);
			case MESSAGE_TYPE_ADD_LOGGED_MSG:
				return new MessageAddLogged(buffer, msgSize);
			case MESSAGE_TYPE_DROPOUT:
				return new MessageDropout(buffer);
			case MESSAGE_TYPE_LOG:
				return new MessageLog(buffer, msgSize);
			case MESSAGE_TYPE_REMOVE_LOGGED_MSG:
			case MESSAGE_TYPE_SYNC:
				buffer.position(buffer.position() + msgSize);
			//	System.err.println("Sync: " +  (char)msgType+":"+msgSize);
				return null;
			default:
				if (msgSize == 0 && msgType == 0) {
					// This is an error (corrupt file): likely the file is filled with zeros from this point on.
					// Not much we can do except to ensure that we make progress and don't spam the error console.
				} else {
					buffer.position(buffer.position()+msgSize);
					//System.out.println((char)msgType);
				}
			}
		} catch(Exception e) {  }
		return null;
	}

	private boolean checkMagicHeader() {
		boolean error = true;
		if ((buffer.get() & 0xFF) != 'U')
			error = false;
		if ((buffer.get() & 0xFF) != 'L')
			error = false;
		if ((buffer.get() & 0xFF) != 'o')
			error = false;
		if ((buffer.get() & 0xFF) != 'g')
			error = false;
		if ((buffer.get() & 0xFF) != 0x01)
			error = false;
		if ((buffer.get() & 0xFF) != 0x12)
			error = false;
		if ((buffer.get() & 0xFF) != 0x35)
			error = false;
		if ((buffer.get() & 0xFF) != 0x00 && !error) {
			MSPLogger.getInstance().writeLocalMsg("[mgc] ULog: Different version than expected. Will try anyway",
					MAV_SEVERITY.MAV_SEVERITY_DEBUG);
		}
		logStartTimestamp = buffer.getLong();
		return error;
	}

	private void applyMsg(Map<String, Object> update, MessageData msg) {
		applyMsgAsName(update, msg, msg.format.name + "_" + msg.multiID);
	}

	private void applyMsgAsName(Map<String, Object> update, MessageData msg, String msg_name) {
		final ArrayList<FieldFormat> fields = msg.format.fields;
		for (int i = 0; i < fields.size(); i++)  {
			FieldFormat field = fields.get(i);
			if (field.isArray()) {
				for (int j = 0; j < field.size; j++) {
					update.put(msg_name + "." + field.name + "[" + j + "]", ((Object[]) msg.get(i))[j]);
				}
			} else {
				update.put(msg_name + "." + field.name, msg.get(i));
			}
		}
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(int[] bytes, int len) {
		char[] hexChars = new char[len * 2];
		for ( int j = 0; j <len; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	// private classes

	private class ParamUpdate {
		private String name;
		private Object value;
		private long timestamp = -1;
		private ParamUpdate(String nm, Object v, long ts) {
			name = nm;
			value = v;
			timestamp = ts;
		}

		public String getName() {
			return name;
		}

		public Object getValue() {
			return value;
		}

		public long getTimestamp() {
			return timestamp;
		}
	}

	private class Subscription {
		public Subscription(MessageFormat f, int multiID) {
			this.format = f;
			this.multiID = multiID;
		}
		public MessageFormat format;
		public int multiID;
	}
}
