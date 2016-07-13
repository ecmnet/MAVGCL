package me.drton.jmavlib.log.ulog;

import me.drton.jmavlib.log.BinaryLogReader;
import me.drton.jmavlib.log.FormatErrorException;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.*;

/**
 * User: ton Date: 03.06.13 Time: 14:18
 */
public class ULogReader extends BinaryLogReader {
	private static final byte SYNC_BYTE = (byte) '>';
	private static final byte MESSAGE_TYPE_FORMAT = (byte) 'F';
	private static final byte MESSAGE_TYPE_DATA = (byte) 'D';
	private static final byte MESSAGE_TYPE_INFO = (byte) 'I';
	private static final byte MESSAGE_TYPE_SUBSCRIBE = (byte) 'A';
	private static final byte MESSAGE_TYPE_UNSUBSCRIBE = (byte) 'R';
	private static final byte MESSAGE_TYPE_LOGMSG     = (byte) 'L';
	private static final byte MESSAGE_TYPE_PARAMETER = (byte) 'P';

	private String systemName = "";
	private long dataStart = 0;
	private Map<String, MessageFormat> messageFormats = new HashMap<String, MessageFormat>();
	private Map<Integer,  MessageFormat> subscribedFormats = new HashMap<Integer,  MessageFormat>();
	private Map<String, String> fieldsList = null;
	private long sizeUpdates = -1;
	private long sizeMicroseconds = -1;
	private long startMicroseconds = -1;
	private long utcTimeReference = -1;
	private Map<String, Object> version = new HashMap<String, Object>();
	private Map<String, Object> parameters = new HashMap<String, Object>();
	private List<Exception> errors = new ArrayList<Exception>();
	private int logVersion = 0;
	private int headerSize = 3;

	public ULogReader(String fileName) throws IOException, FormatErrorException {
		super(fileName);
		updateStatistics();
	}

	@Override
	public String getFormat() {
		return "ULog v" + logVersion;
	}

	public String getSystemName() {
		return systemName;
	}

	@Override
	public long getSizeUpdates() {
		return sizeUpdates;
	}

	@Override
	public long getStartMicroseconds() {
		return startMicroseconds;
	}

	@Override
	public long getSizeMicroseconds() {
		return sizeMicroseconds;
	}

	@Override
	public long getUTCTimeReferenceMicroseconds() {
		return utcTimeReference;
	}

	@Override
	public Map<String, Object> getVersion() {
		return version;
	}

	@Override
	public Map<String, Object> getParameters() {
		return parameters;
	}

	private void updateStatistics() throws IOException, FormatErrorException {

		position(0);
		fillBuffer(8);
		byte[] logVersionBytes = new byte[8];
		buffer.get(logVersionBytes);
		logVersion =  logVersionBytes[7];
		headerSize = 3;

		fillBuffer(8);
		long logtime = buffer.getLong();
		long packetsNum = 0;
		long timeStart = -1;
		long timeEnd = -1;
		fieldsList = new HashMap<String, String>();
		while (true) {
			Object msg;
			try {
				msg = readMessage();
			} catch (EOFException e) {
				break;
			}
			packetsNum++;

			if (msg instanceof MessageFormat) {
				MessageFormat msgFormat = (MessageFormat) msg;
				messageFormats.put(msgFormat.name, msgFormat);
				if (!msgFormat.name.isEmpty() && msgFormat.name.charAt(0) != '_') {
					for (int i = 0; i < msgFormat.fields.length; i++) {
						FieldFormat fieldDescr = msgFormat.fields[i];
						if (fieldDescr.isArray()) {
							for (int j = 0; j < fieldDescr.size; j++) {
								fieldsList.put(msgFormat.name + "." + fieldDescr.name + "[" + j + "]", fieldDescr.type);
							}
						} else {
							fieldsList.put(msgFormat.name + "." + fieldDescr.name, fieldDescr.type);
						}
					}
				}

			} else if (msg instanceof MessageParameter) {
				MessageParameter msgParam = (MessageParameter) msg;
				parameters.put(msgParam.getKey(), msgParam.value);

			} else if (msg instanceof MessageInfo) {
				MessageInfo msgInfo = (MessageInfo) msg;
				if ("sys_name".equals(msgInfo.getKey())) {
					systemName = (String) msgInfo.value;
					version.put("SYS", msgInfo.value);
				} else if ("ver_hw".equals(msgInfo.getKey())) {
					version.put("HW", msgInfo.value);
				} else if ("ver_sw".equals(msgInfo.getKey())) {
					version.put("FW", msgInfo.value);
				} else if ("time_ref_utc".equals(msgInfo.getKey())) {
					utcTimeReference = ((Number) msgInfo.value).longValue();
				}

			} else if (msg instanceof MessageSubscribe) {

				MessageSubscribe msgSub = (MessageSubscribe) msg;
				subscribedFormats.put(msgSub.msgID, messageFormats.get(msgSub.formatName));

			} else if (msg instanceof MessageUnsubscribe) {
				MessageUnsubscribe msgUsub = (MessageUnsubscribe) msg;
				subscribedFormats.remove(msgUsub.msgID);

			} else if (msg instanceof MessageData) {
				if (dataStart == 0) {
					dataStart = position();
				}
				MessageData msgData = (MessageData) msg;
				if(timeStart < 0)
					timeStart = (long)msgData.get("timestamp");
				long t = (long)msgData.get("timestamp");
				if(t>timeEnd)
					timeEnd = t;
			}
		}
		startMicroseconds = timeStart;
		sizeUpdates = packetsNum;
		sizeMicroseconds = timeEnd - timeStart;
		System.out.println(packetsNum+" packets read. LogSize is "+sizeMicroseconds / 1000000 +"sec");
		position(dataStart);
	}

	@Override
    public boolean seek(long seekTime) throws IOException, FormatErrorException {
        position(dataStart);
        if (seekTime == 0) {      // Seek to start of log
            return true;
        }
        // Seek to specified timestamp without parsing all messages
        try {
            while (true) {
                fillBuffer(headerSize);
                long pos = position();
                if (logVersion > 0) {
                    byte sync = buffer.get();
                    if (sync != SYNC_BYTE) {
                        continue;
                    }
                }
                int msgType = buffer.get() & 0xFF;
                int msgSize;
                if (logVersion == 0) {
                    msgSize = buffer.get() & 0xFF;
                } else {
                    msgSize = buffer.getShort() & 0xFFFF;
                }
                fillBuffer(msgSize);
                if (msgType == MESSAGE_TYPE_DATA) {
                    buffer.get();   // MsgID
                    buffer.get();   // MultiID
                    long timestamp = buffer.getLong();
                    if (timestamp >= seekTime) {
                        // Time found
                        position(pos);
                        return true;
                    }
                    buffer.position(buffer.position() + msgSize - 10);
                } else {
                    fillBuffer(msgSize);
                    buffer.position(buffer.position() + msgSize);
                }
            }
        } catch (EOFException e) {
            return false;
        }
    }

	private void applyMsg(Map<String, Object> update, MessageData msg) {

		applyMsgAsName(update, msg, msg.format.name);

	}

	void applyMsgAsName(Map<String, Object> update, MessageData msg, String msg_name) {
		FieldFormat[] fields = msg.format.fields;
		for (int i = 0; i < fields.length; i++) {
			FieldFormat field = fields[i];
			if(field.name.startsWith("_p"))
				continue;
			if (field.isArray()) {
				for (int j = 0; j < field.size; j++) {
					update.put(msg_name + "." + field.name + "[" + j + "]", ((Object[]) msg.get(i))[j]);
				}
			} else {
				update.put(msg_name + "." + field.name, msg.get(i));
			}
		}
	}

	@Override
	public long readUpdate(Map<String, Object> update) throws IOException, FormatErrorException {
		while (true) {
			Object msg = readMessage();
			if (msg instanceof MessageData) {
				applyMsg(update, (MessageData) msg);
				return (long)((MessageData) msg).get("timestamp");
			}
//			else if(msg instanceof MessageLogMsg) {
//                update.put("msg", ((MessageLogMsg) msg).message);
//                update.put("msglevel",  ((MessageLogMsg) msg).loglevel);
//                return (long)((MessageLogMsg) msg).tms;
//			}
		}
	}

	@Override
	public Map<String, String> getFields() {
		return fieldsList;
	}

	/**
	 * Read next message from log
	 *
	 * @return log message
	 * @throws IOException  on IO error
	 * @throws EOFException on end of stream
	 */
	public Object readMessage() throws IOException, FormatErrorException {
		while (true) {
			fillBuffer(headerSize);
			long pos = position();

			int msgSize = buffer.getShort();
			int msgType = buffer.get() & 0xFF;

			try {
				fillBuffer(msgSize);
			} catch (EOFException e) {
				errors.add(new FormatErrorException(pos, "Unexpected end of file"));
				throw e;
			}
			Object msg=null;
			try {
				if (msgType == MESSAGE_TYPE_DATA) {
					int msgID = buffer.getShort() & 0xFFFF;
					MessageFormat msgFormat = subscribedFormats.get(msgID);
					if (msgFormat == null) {
						errors.add(new FormatErrorException(pos, "Unknown DATA message ID: " + msgID));
						buffer.position(buffer.position() + msgSize - 2);
						continue;
					} else {
						msg = new MessageData(msgFormat, buffer);
					}
				} else if (msgType == MESSAGE_TYPE_INFO) {
					msg = new MessageInfo(buffer);
				} else if (msgType == MESSAGE_TYPE_PARAMETER) {
					msg = new MessageParameter(buffer);
				} else if (msgType == MESSAGE_TYPE_LOGMSG) {
					msg = new MessageLogMsg(buffer,msgSize);
				} else if (msgType == MESSAGE_TYPE_FORMAT) {
					msg = new MessageFormat(buffer, msgSize);
				} else if (msgType == MESSAGE_TYPE_SUBSCRIBE) {
					msg = new MessageSubscribe(buffer,msgSize);
				} else if (msgType == MESSAGE_TYPE_UNSUBSCRIBE) {
					msg = new MessageUnsubscribe(buffer,msgSize);
				} else {
					buffer.position(buffer.position() + msgSize);
					errors.add(new FormatErrorException(pos, "Unknown message type: " + msgType));
					continue;
				}
			} catch (Exception e) {
				errors.add(new FormatErrorException(pos, "Error parsing message type: " +e.getMessage(), e.getCause()));
				int sizeParsed = (int) (position() - pos - headerSize);
				buffer.position(buffer.position() + msgSize - sizeParsed);
				continue;
			}
			int sizeParsed = (int) (position() - pos - headerSize);
			if (sizeParsed != msgSize) {
				if(msg instanceof MessageData)
					errors.add(new FormatErrorException(pos, "Message size mismatch, parsed: " + sizeParsed + ", msg size: " + msgSize
							+" --> "+((MessageData)msg).format.name));
				else
					errors.add(new FormatErrorException(pos, "Message size mismatch, parsed: " + sizeParsed + ", msg size: " + msgSize));

				buffer.position(buffer.position() + msgSize - sizeParsed);
			}
			return msg;
		}
	}


	@Override
	public List<Exception> getErrors() {
		return errors;
	}

	@Override
	public void clearErrors() {
		errors.clear();
	}
}
