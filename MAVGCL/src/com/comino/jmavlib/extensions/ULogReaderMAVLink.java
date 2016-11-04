package com.comino.jmavlib.extensions;

import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JFileChooser;

import me.drton.jmavlib.log.BinaryLogReader;
import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.ulog.FieldFormat;
import me.drton.jmavlib.log.ulog.MessageAddLogged;
import me.drton.jmavlib.log.ulog.MessageData;
import me.drton.jmavlib.log.ulog.MessageDropout;
import me.drton.jmavlib.log.ulog.MessageFormat;
import me.drton.jmavlib.log.ulog.MessageInfo;
import me.drton.jmavlib.log.ulog.MessageLog;
import me.drton.jmavlib.log.ulog.MessageParameter;
import me.drton.jmavlib.log.ulog.ULogReader;

/**
 * User: ton Date: 03.06.13 Time: 14:18
 */
public class ULogReaderMAVLink extends MAVLinkLogReader {
    static final byte MESSAGE_TYPE_FORMAT = (byte) 'F';
    static final byte MESSAGE_TYPE_DATA = (byte) 'D';
    static final byte MESSAGE_TYPE_INFO = (byte) 'I';
    static final byte MESSAGE_TYPE_PARAMETER = (byte) 'P';
    static final byte MESSAGE_TYPE_ADD_LOGGED_MSG = (byte) 'A';
    static final byte MESSAGE_TYPE_REMOVE_LOGGED_MSG = (byte) 'R';
    static final byte MESSAGE_TYPE_SYNC = (byte) 'S';
    static final byte MESSAGE_TYPE_DROPOUT = (byte) 'O';
    static final byte MESSAGE_TYPE_LOG = (byte) 'L';
    static final int HDRLEN = 3;
    static final int FILE_MAGIC_HEADER_LENGTH = 16;

    private String systemName = "PX4";
    private long dataStart = 0;
    private Map<String, MessageFormat> messageFormats = new HashMap<String, MessageFormat>();

    private class Subscription {
        public Subscription(MessageFormat f, int multiID) {
            this.format = f;
            this.multiID = multiID;
        }
        public MessageFormat format;
        public int multiID;
    }

    /** all subscriptions. Index is the message id */
    private ArrayList<Subscription> messageSubscriptions = new ArrayList<Subscription>();

    private Map<String, String> fieldsList = null;
    private long sizeUpdates = -1;
    private long sizeMicroseconds = -1;
    private long startMicroseconds = -1;
    private long utcTimeReference = -1;
    private long logStartTimestamp = -1;
    private boolean nestedParsingDone = false;
    private Map<String, Object> version = new HashMap<String, Object>();
    private Map<String, Object> parameters = new HashMap<String, Object>();
    public ArrayList<MessageLog> loggedMessages = new ArrayList<MessageLog>();

    public Map<String, List<ParamUpdate>> parameterUpdates;
    public class ParamUpdate {
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
    private List<Exception> errors = new ArrayList<Exception>();

    /** Index for fast(er) seeking */
    private ArrayList<SeekTime> seekTimes = null;

    private class SeekTime {
        public SeekTime(long t, long pos) {
            timestamp = t;
            position = pos;
        }

        public long timestamp;
        public long position;
    }

    public ULogReaderMAVLink()  {
        super();
        parameterUpdates = new HashMap<String, List<ParamUpdate>>();
    }

    @Override
    public String getFormat() {
        return "ULog";
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

    /**
     * Read and parse the file header.
     *
     * @throws IOException
     * @throws FormatErrorException
     */
    private void readFileHeader() throws IOException, FormatErrorException {
        fillBuffer(FILE_MAGIC_HEADER_LENGTH);
        //magic + version
        boolean error = false;
        if ((buffer.get() & 0xFF) != 'U')
            error = true;
        if ((buffer.get() & 0xFF) != 'L')
            error = true;
        if ((buffer.get() & 0xFF) != 'o')
            error = true;
        if ((buffer.get() & 0xFF) != 'g')
            error = true;
        if ((buffer.get() & 0xFF) != 0x01)
            error = true;
        if ((buffer.get() & 0xFF) != 0x12)
            error = true;
        if ((buffer.get() & 0xFF) != 0x35)
            error = true;
        if ((buffer.get() & 0xFF) != 0x00 && !error) {
            System.out.println("ULog: Different version than expected. Will try anyway");
        }
        if (error)
            throw new FormatErrorException("ULog: Wrong file format");

        logStartTimestamp = buffer.getLong();
    }

    /**
     * Read all necessary information from the file, including message formats,
     * seeking positions and log file information.
     *
     * @throws IOException
     * @throws FormatErrorException
     */
    public void updateStatistics() throws IOException, FormatErrorException {
        position(0);
        readFileHeader();
        long packetsNum = 0;
        long timeStart = -1;
        long timeEnd = -1;
        long lastTime = -1;
        fieldsList = new HashMap<String, String>();
        seekTimes = new ArrayList<SeekTime>();
        while (true) {
            Object msg;
            long pos = position();
            try {
                msg = readMessage();
            } catch (EOFException e) {
                break;
            }
            packetsNum++;

            if (msg instanceof MessageFormat) {
                MessageFormat msgFormat = (MessageFormat) msg;
                messageFormats.put(msgFormat.name, msgFormat);

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
                if(msgFormat == null)
                    throw new FormatErrorException("Format of subscribed message not found: " + msgAddLogged.name);
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
                if (parameters.containsKey(msgParam.getKey())) {
                    System.out.println("update to parameter: " + msgParam.getKey() + " value: " + msgParam.value + " at t = " + lastTime);
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
                    version.put("HW", msgInfo.value);
                } else if ("ver_sw".equals(msgInfo.getKey())) {
                    version.put("FW", msgInfo.value);
                } else if ("time_ref_utc".equals(msgInfo.getKey())) {
                    utcTimeReference = ((long) ((Number) msgInfo.value).intValue()) * 1000 * 1000;
                }

            } else if (msg instanceof MessageData) {
                if (dataStart == 0) {
                    dataStart = pos;
                }
                MessageData msgData = (MessageData) msg;
                seekTimes.add(new SeekTime(msgData.timestamp, pos));

                if (timeStart < 0) {
                    timeStart = msgData.timestamp;
                }
                if (timeEnd < msgData.timestamp) timeEnd = msgData.timestamp;
                lastTime = msgData.timestamp;
            } else if (msg instanceof MessageLog) {
                MessageLog msgLog = (MessageLog) msg;
                loggedMessages.add(msgLog);
            }
        }

        // fill the fieldsList now that we know how many multi-instances are in the log
        for (int k = 0; k < messageSubscriptions.size(); ++k) {
            Subscription s = messageSubscriptions.get(k);
            if (s != null) {
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
        startMicroseconds = timeStart;
        sizeUpdates = packetsNum;
        sizeMicroseconds = timeEnd - timeStart;
        seek(0);

        if (!errors.isEmpty()) {
            System.err.println("Errors while reading file:");
            for (final Exception e : errors) {
                System.err.println(e.getMessage());
            }
            errors.clear();
        }
    }

    @Override
    public boolean seek(long seekTime) throws IOException, FormatErrorException {
        position(dataStart);
        if (seekTime == 0) {      // Seek to start of log
            return true;
        }

        //find the position in seekTime. We could speed this up further by
        //using a binary search
        for (SeekTime sk : seekTimes) {
            if (sk.timestamp >= seekTime) {
                position(sk.position);
                return true;
            }
        }
        return false;
    }

    private void applyMsg(Map<String, Object> update, MessageData msg) {
        applyMsgAsName(update, msg, msg.format.name + "_" + msg.multiID);
    }

    void applyMsgAsName(Map<String, Object> update, MessageData msg, String msg_name) {
        final ArrayList<FieldFormat> fields = msg.format.fields;
        for (int i = 0; i < fields.size(); i++) {
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
            fillBuffer(HDRLEN);
            long pos = position();
            int s1 = buffer.get() & 0xFF;
            int s2 = buffer.get() & 0xFF;
            int msgSize = s1 + (256 * s2);
            int msgType = buffer.get() & 0xFF;
            try {
                fillBuffer(msgSize);
            } catch (EOFException e) {
                errors.add(new FormatErrorException(pos, "Unexpected end of file"));
                throw e;
            }
            Object msg;
            switch (msgType) {
            case MESSAGE_TYPE_DATA:
                s1 = buffer.get() & 0xFF;
                s2 = buffer.get() & 0xFF;
                int msgID = s1 + (256 * s2);
                Subscription subscription = null;
                if (msgID < messageSubscriptions.size())
                    subscription = messageSubscriptions.get(msgID);
                if (subscription == null) {
                    position(pos);
                    errors.add(new FormatErrorException(pos, "Unknown DATA subscription ID: " + msgID));
                    buffer.position(buffer.position() + msgSize - 1);
                    continue;
                }
                msg = new MessageData(subscription.format, buffer, subscription.multiID);
                break;
            case MESSAGE_TYPE_INFO:
                msg = new MessageInfo(buffer);
                break;
            case MESSAGE_TYPE_PARAMETER:
                msg = new MessageParameter(buffer);
                break;
            case MESSAGE_TYPE_FORMAT:
                msg = new MessageFormat(buffer, msgSize);
                break;
            case MESSAGE_TYPE_ADD_LOGGED_MSG:
                msg = new MessageAddLogged(buffer, msgSize);
                break;
            case MESSAGE_TYPE_DROPOUT:
                msg = new MessageDropout(buffer);
                break;
            case MESSAGE_TYPE_LOG:
                msg = new MessageLog(buffer, msgSize);
                break;
            case MESSAGE_TYPE_REMOVE_LOGGED_MSG:
            case MESSAGE_TYPE_SYNC:
                buffer.position(buffer.position() + msgSize); //skip this message
                continue;
            default:
                buffer.position(buffer.position() + msgSize);
                errors.add(new FormatErrorException(pos, "Unknown message type: " + msgType));
                continue;
            }
            int sizeParsed = (int) (position() - pos - HDRLEN);
            if (sizeParsed != msgSize) {
                errors.add(new FormatErrorException(pos, "Message size mismatch, parsed: " + sizeParsed + ", msg size: " + msgSize));
                buffer.position(buffer.position() + msgSize - sizeParsed);
            }
            return msg;
        }
    }

    @Override
    public List<Exception> getErrors() {
        return Collections.emptyList();
    }

    @Override
    public void clearErrors() {
    }
}
