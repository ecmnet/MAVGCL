package me.drton.jmavlib.log.ulog;

import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import javax.swing.JFileChooser;

import me.drton.jmavlib.log.BinaryLogReader;
import me.drton.jmavlib.log.FormatErrorException;

/**
 * User: ton Date: 03.06.13 Time: 14:18
 */
public class ULogReader extends BinaryLogReader {
    static final byte MESSAGE_TYPE_FORMAT = (byte) 'F';
    static final byte MESSAGE_TYPE_DATA = (byte) 'D';
    static final byte MESSAGE_TYPE_INFO = (byte) 'I';
    static final byte MESSAGE_TYPE_INFO_MULTIPLE = (byte) 'M';
    static final byte MESSAGE_TYPE_PARAMETER = (byte) 'P';
    static final byte MESSAGE_TYPE_PARAMETER_DEFAULT = (byte) 'Q';
    static final byte MESSAGE_TYPE_ADD_LOGGED_MSG = (byte) 'A';
    static final byte MESSAGE_TYPE_REMOVE_LOGGED_MSG = (byte) 'R';
    static final byte MESSAGE_TYPE_SYNC = (byte) 'S';
    static final byte MESSAGE_TYPE_DROPOUT = (byte) 'O';
    static final byte MESSAGE_TYPE_LOG = (byte) 'L';
    static final byte MESSAGE_TYPE_FLAG_BITS = (byte) 'B';
    static final int HDRLEN = 3;
    static final int FILE_MAGIC_HEADER_LENGTH = 16;

    static final int INCOMPAT_FLAG0_DATA_APPENDED_MASK = 1 << 0;

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

    private String hardfaultPlainText = "";

    private Vector<Long> appendedOffsets = new Vector<Long>();
    int currentAppendingOffsetIndex =
        0; // current index to appendedOffsets for the next appended offset

    public Map<String, List<ParamUpdate>> parameterUpdates;
    private boolean replayedLog = false;
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
    private int logVersion = 0;
    private int headerSize = 2;

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

    public ULogReader(String fileName) throws IOException, FormatErrorException {
        super(fileName);
        parameterUpdates = new HashMap<String, List<ParamUpdate>>();
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
    
    public Map<String, String> getFieldList() {
		return fieldsList;
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
        if ((buffer.get() & 0xFF) != 'U') {
            error = true;
        }
        if ((buffer.get() & 0xFF) != 'L') {
            error = true;
        }
        if ((buffer.get() & 0xFF) != 'o') {
            error = true;
        }
        if ((buffer.get() & 0xFF) != 'g') {
            error = true;
        }
        if ((buffer.get() & 0xFF) != 0x01) {
            error = true;
        }
        if ((buffer.get() & 0xFF) != 0x12) {
            error = true;
        }
        if ((buffer.get() & 0xFF) != 0x35) {
            error = true;
        }
        if ((buffer.get() & 0xFF) > 0x01 && !error) {
            System.out.println("ULog: Different version than expected. Will try anyway");
        }
        if (error) {
            throw new FormatErrorException("ULog: Wrong file format");
        }

        logStartTimestamp = buffer.getLong();
    }

    /**
     * Read all necessary information from the file, including message formats,
     * seeking positions and log file information.
     *
     * @throws IOException
     * @throws FormatErrorException
     */
    private void updateStatistics() throws IOException, FormatErrorException {
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
                if ((msgFlags.incompatFlags[0] & ~0x1) != 0) {
                    containsUnknownIncompatBits = true;
                }
                for (int i = 1; i < msgFlags.incompatFlags.length; ++i) {
                    if (msgFlags.incompatFlags[i] != 0) {
                        containsUnknownIncompatBits = true;
                    }
                }
                if (containsUnknownIncompatBits) {
                    throw new FormatErrorException("Log contains unknown incompatible bits. Refusing to parse the log.");
                }

            } else if (msg instanceof MessageFormat) {
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
                if (msgFormat == null) {
                    throw new FormatErrorException("Format of subscribed message not found: " + msgAddLogged.name);
                }
                Subscription subscription = new Subscription(msgFormat, msgAddLogged.multiID);
                if (msgAddLogged.msgID < messageSubscriptions.size()) {
                    messageSubscriptions.set(msgAddLogged.msgID, subscription);
                } else {
                    while (msgAddLogged.msgID > messageSubscriptions.size()) {
                        messageSubscriptions.add(null);
                    }
                    messageSubscriptions.add(subscription);
                }
                if (msgAddLogged.multiID > msgFormat.maxMultiID) {
                    msgFormat.maxMultiID = msgAddLogged.multiID;
                }

            } else if (msg instanceof MessageParameter) {
                MessageParameter msgParam = (MessageParameter) msg;
                // a replayed log can contain many parameter updates, so we ignore them here
                if (parameters.containsKey(msgParam.getKey()) && !replayedLog) {
                    System.out.println("update to parameter: " + msgParam.getKey() + " value: " + msgParam.value +
                                       " at t = " + lastTime);
                    // maintain a record of parameters which change during flight
                    if (parameterUpdates.containsKey(msgParam.getKey())) {
                        parameterUpdates.get(msgParam.getKey()).add(new ParamUpdate(msgParam.getKey(), msgParam.value,
                                                                                    lastTime));
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
                    utcTimeReference = ((long)((Number) msgInfo.value).intValue()) * 1000 * 1000;
                } else if ("replay".equals(msgInfo.getKey())) {
                    replayedLog = true;
                }
            } else if (msg instanceof MessageInfoMultiple) {
                MessageInfoMultiple msgInfo = (MessageInfoMultiple) msg;
                //System.out.println(msgInfo.getKey());
                if ("hardfault_plain".equals(msgInfo.getKey())) {
                    // append all hardfaults to one String (we should be looking at msgInfo.isContinued as well)
                    hardfaultPlainText += (String)msgInfo.value;
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
                if (timeEnd < msgData.timestamp) { timeEnd = msgData.timestamp; }
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

        if (hardfaultPlainText.length() > 0) {
            // TODO: find a better way to show this to the user?
            System.out.println("Log contains hardfault data:");
            System.out.println(hardfaultPlainText);
        }
    }

    @Override
    public boolean seek(long seekTime) throws IOException, FormatErrorException {
        position(dataStart);
        currentAppendingOffsetIndex = 0;

        if (seekTime == 0) {      // Seek to start of log
            return true;
        }

        //find the position in seekTime. We could speed this up further by
        //using a binary search
        for (SeekTime sk : seekTimes) {
            if (sk.timestamp >= seekTime) {
                position(sk.position);
                while (currentAppendingOffsetIndex < appendedOffsets.size() &&
                        appendedOffsets.get(currentAppendingOffsetIndex) < sk.position) {
                    ++currentAppendingOffsetIndex;
                }
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

            // check if we cross an appending boundary: if so, we need to reset the position and skip this message
            if (currentAppendingOffsetIndex < appendedOffsets.size()) {
                if (pos + HDRLEN + msgSize > appendedOffsets.get(currentAppendingOffsetIndex)) {
                    //System.out.println("Jumping to next position: "+pos + ", next: "+appendedOffsets.get(currentAppendingOffsetIndex));
                    position(appendedOffsets.get(currentAppendingOffsetIndex));
                    ++currentAppendingOffsetIndex;
                    continue;
                }
            }

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
                    if (msgID < messageSubscriptions.size()) {
                        subscription = messageSubscriptions.get(msgID);
                    }
                    if (subscription == null) {
                        position(pos);
                        errors.add(new FormatErrorException(pos, "Unknown DATA subscription ID: " + msgID));
                        buffer.position(buffer.position() + msgSize - 1);
                        continue;
                    }
                    msg = new MessageData(subscription.format, buffer, subscription.multiID);
                    break;
                case MESSAGE_TYPE_FLAG_BITS:
                    msg = new MessageFlagBits(buffer, msgSize);
                    break;
                case MESSAGE_TYPE_INFO:
                    msg = new MessageInfo(buffer);
                    break;
                case MESSAGE_TYPE_INFO_MULTIPLE:
                    msg = new MessageInfoMultiple(buffer);
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
                case MESSAGE_TYPE_PARAMETER_DEFAULT:
                    buffer.position(buffer.position() + msgSize); //skip this message
                    continue;
                default:
                    if (msgSize == 0 && msgType == 0) {
                        // This is an error (corrupt file): likely the file is filled with zeros from this point on.
                        // Not much we can do except to ensure that we make progress and don't spam the error console.
                    } else {
                        buffer.position(buffer.position() + msgSize);
                        errors.add(new FormatErrorException(pos, "Unknown message type: " + msgType));
                    }
                    continue;
            }
            int sizeParsed = (int)(position() - pos - HDRLEN);
            if (sizeParsed != msgSize) {
                errors.add(new FormatErrorException(pos,
                                                    "Message size mismatch, parsed: " + sizeParsed + ", msg size: " + msgSize));
                buffer.position(buffer.position() + msgSize - sizeParsed);
            }
            return msg;
        }
    }

    /*
    Dump each stream of message data records to a CSV file named "topic_N.csv"
    First line of each file is "timestamp,field1,field2,..."
     */
    public static void main(String[] args) throws Exception {
        ULogReader reader = null;
        JFileChooser openLogFileChooser = new JFileChooser();
        String basePath = "/home/markw/gdrive/flightlogs/logger";
        openLogFileChooser.setCurrentDirectory(new File(basePath));
        int returnVal = openLogFileChooser.showDialog(null, "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = openLogFileChooser.getSelectedFile();
            String logFileName = file.getPath();
            basePath = file.getParent();
            reader = new ULogReader(logFileName);
        } else {
            System.exit(0);
        }
        // write all parameters to a gnu Octave data file
        FileWriter fileWriter = new FileWriter(new File(basePath + File.separator + "parameters.text"));
        Map<String, Object> tmap = new TreeMap<String, Object>(reader.parameters);
        Set pSet = tmap.entrySet();
        for (Object aPSet : pSet) {
            Map.Entry param = (Map.Entry) aPSet;
            fileWriter.write(String.format("# name: %s\n#type: scalar\n%s\n", param.getKey(),
                                           param.getValue()));
        }
        fileWriter.close();
        long tStart = System.currentTimeMillis();
        double last_t = 0;
        double last_p = 0;
        Map<String, PrintStream> ostream = new HashMap<String, PrintStream>();
        Map<String, Double> lastTimeStamp = new HashMap<String, Double>();
        double min_dt = 1;
        while (true) {
//            try {
//                Object msg = reader.readMessage();
//                System.out.println(msg);
//            } catch (EOFException e) {
//                break;
//            }
            Map<String, Object> update = new HashMap<String, Object>();
            try {
                long t = reader.readUpdate(update);
                double tsec = (double)t / 1e6;
                if (tsec > (last_p + 1)) {
                    last_p = tsec;
                    System.out.printf("%8.0f\n", tsec);
                }
                // keys in Map "update" are fieldnames beginning with the topic name e.g. SENSOR_GYRO_0.someField
                // Create a printstream for each topic when it is first encountered
                Set<String> keySet = update.keySet();
                String stream = keySet.iterator().next().split("\\.")[0];
                if (!ostream.containsKey(stream)) {
                    System.out.println("creating stream " + stream);
                    PrintStream newStream = new PrintStream(basePath + File.separator + stream + ".csv");
                    ostream.put(stream, newStream);
                    lastTimeStamp.put(stream, tsec);
                    Iterator<String> keys = keySet.iterator();
                    newStream.print("timestamp");
                    while (keys.hasNext()) {
                        String fieldName = keys.next();
                        if (!fieldName.contains("_padding") && fieldName != "timestamp") {
                            newStream.print(',');
                            newStream.print(fieldName);
                        }
                    }
                    newStream.println();
                }
                // append this record to output stream
                PrintStream curStream = ostream.get(stream);
                // timestamp is always first entry in record
                curStream.print(t);
                // for each non-padding field, print value
                Iterator<String> keys = keySet.iterator();
                while (keys.hasNext()) {
                    String fieldName = keys.next();
                    if (!fieldName.contains("_padding") && fieldName != "timestamp") {
                        curStream.print(',');
                        curStream.print(update.get(fieldName));
                    }
                }
//                for (Object field: update.values()) {
//                    curStream.print(',');
//                    curStream.print(field.toString());
//                }
                curStream.println();
                // check gyro stream for dropouts
                if (stream.startsWith("SENSOR_GYRO")) {
                    double dt = tsec - lastTimeStamp.get(stream);
                    double rdt = Math.rint(1000 * dt) / 1000;
                    if ((dt > 0) && (rdt < min_dt)) {
                        min_dt = rdt;
                        System.out.println("rdt: " + rdt);
                    }
                    if (dt > (5 * min_dt)) {
                        System.out.println("gyro dropout: " + lastTimeStamp.get(stream) + ", length: " + dt);
                    }
                    lastTimeStamp.put(stream, tsec);
                }
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
