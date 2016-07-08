package me.drton.jmavlib.log.ulog;

import me.drton.jmavlib.log.BinaryLogReader;
import me.drton.jmavlib.log.FormatErrorException;

import java.io.EOFException;
import java.io.IOException;
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
    private static final byte MESSAGE_TYPE_PARAMETER = (byte) 'P';

    private String systemName = "";
    private long dataStart = 0;
    private Map<Integer, MessageFormat> messageFormats = new HashMap<Integer, MessageFormat>();
    private Map<String, String> fieldsList = null;
    private long sizeUpdates = -1;
    private long sizeMicroseconds = -1;
    private long startMicroseconds = -1;
    private long utcTimeReference = -1;
    private Map<String, Object> version = new HashMap<String, Object>();
    private Map<String, Object> parameters = new HashMap<String, Object>();
    private List<Exception> errors = new ArrayList<Exception>();
    private int logVersion = 0;
    private int headerSize = 2;

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
        fillBuffer(4);
        byte[] logVersionBytes = new byte[4];
        buffer.get(logVersionBytes);
        String logVersionStr = new String(logVersionBytes, Charset.forName("latin1"));
        if (logVersionStr.startsWith("ULG")) {
            logVersion = Integer.parseInt(logVersionStr.substring(3));
            headerSize = 4;
        } else {
            logVersion = 0;
            headerSize = 2;
            position(0);
        }
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
                messageFormats.put(msgFormat.msgID, msgFormat);
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
                } else if ("ver_hw".equals(msgInfo.getKey())) {
                    version.put("HW", msgInfo.value);
                } else if ("ver_sw".equals(msgInfo.getKey())) {
                    version.put("FW", msgInfo.value);
                } else if ("time_ref_utc".equals(msgInfo.getKey())) {
                    utcTimeReference = ((Number) msgInfo.value).longValue();
                }

            } else if (msg instanceof MessageData) {
                if (dataStart == 0) {
                    dataStart = position();
                }
                MessageData msgData = (MessageData) msg;
                if (timeStart < 0) {
                    timeStart = msgData.timestamp;
                }
                timeEnd = msgData.timestamp;
            }
        }
        startMicroseconds = timeStart;
        sizeUpdates = packetsNum;
        sizeMicroseconds = timeEnd - timeStart;
        seek(0);
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
        applyMsgAsName(update, msg, msg.format.name + "[" + msg.multiID + "]");
        if (msg.isActive) {
            applyMsgAsName(update, msg, msg.format.name);
        }
    }

    void applyMsgAsName(Map<String, Object> update, MessageData msg, String msg_name) {
        FieldFormat[] fields = msg.format.fields;
        for (int i = 0; i < fields.length; i++) {
            FieldFormat field = fields[i];
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
                return ((MessageData) msg).timestamp;
            }
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
            if (logVersion > 0) {
                byte sync = buffer.get();
                if (sync != SYNC_BYTE) {
                    errors.add(new FormatErrorException(pos, String.format("Wrong sync byte: 0x%02X (expected 0x%02X)", sync & 0xFF, SYNC_BYTE & 0xFF)));
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
            try {
                fillBuffer(msgSize);
            } catch (EOFException e) {
                errors.add(new FormatErrorException(pos, "Unexpected end of file"));
                throw e;
            }
            Object msg;
            try {
                if (msgType == MESSAGE_TYPE_DATA) {
                    int msgID = buffer.get() & 0xFF;
                    MessageFormat msgFormat = messageFormats.get(msgID);
                    if (msgFormat == null) {
                        errors.add(new FormatErrorException(pos, "Unknown DATA message ID: " + msgID));
                        buffer.position(buffer.position() + msgSize - 1);
                        continue;
                    } else {
                        msg = new MessageData(msgFormat, buffer);
                    }
                } else if (msgType == MESSAGE_TYPE_INFO) {
                    msg = new MessageInfo(buffer);
                } else if (msgType == MESSAGE_TYPE_PARAMETER) {
                    msg = new MessageParameter(buffer);
                } else if (msgType == MESSAGE_TYPE_FORMAT) {
                    msg = new MessageFormat(buffer, logVersion);
                } else {
                    buffer.position(buffer.position() + msgSize);
                    errors.add(new FormatErrorException(pos, "Unknown message type: " + msgType));
                    continue;
                }
            } catch (Exception e) {
                errors.add(new FormatErrorException(pos, "Error parsing message type: " + msgType, e));
                continue;
            }
            int sizeParsed = (int) (position() - pos - headerSize);
            if (sizeParsed != msgSize) {
                errors.add(new FormatErrorException(pos, "Message size mismatch, parsed: " + sizeParsed + ", msg size: " + msgSize));
                buffer.position(buffer.position() + msgSize - sizeParsed);
            }
            return msg;
        }
    }

    public static void main(String[] args) throws Exception {
        ULogReader reader = new ULogReader("test.ulg");
        long tStart = System.currentTimeMillis();
        while (true) {
//            try {
//                Object msg = reader.readMessage();
//                System.out.println(msg);
//            } catch (EOFException e) {
//                break;
//            }
            Map<String, Object> update = new HashMap<String, Object>();
            try {
                reader.readUpdate(update);
//                System.out.println(update);
            } catch (EOFException e) {
                break;
            }
        }
        long tEnd = System.currentTimeMillis();
        for (Exception e : reader.getErrors()) {
            e.printStackTrace();
        }
        System.out.println(tEnd - tStart);
        reader.close();
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
