package me.drton.jmavlib.log.px4;

import me.drton.jmavlib.log.BinaryLogReader;
import me.drton.jmavlib.log.FormatErrorException;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;

/**
 * User: ton Date: 03.06.13 Time: 14:18
 */
public class PX4LogReader extends BinaryLogReader {
    private static final int HEADER_LEN = 3;
    private static final byte HEADER_HEAD1 = (byte) 0xA3;
    private static final byte HEADER_HEAD2 = (byte) 0x95;

    private long dataStart = 0;
    private boolean formatPX4 = false;
    private Map<Integer, PX4LogMessageDescription> messageDescriptions
            = new HashMap<Integer, PX4LogMessageDescription>();
    private Map<String, String> fieldsList = null;
    private long time = 0;
    private PX4LogMessage lastMsg = null;
    private long sizeUpdates = -1;
    private long sizeMicroseconds = -1;
    private long startMicroseconds = -1;
    private long utcTimeReference = -1;
    private Map<String, Object> version = new HashMap<String, Object>();
    private Map<String, Object> parameters = new HashMap<String, Object>();
    private List<Exception> errors = new ArrayList<Exception>();
    private String tsName = null;
    private boolean tsMicros;

    private static Set<String> hideMsgs = new HashSet<String>();
    private static Map<String, String> formatNames = new HashMap<String, String>();

    static {
        hideMsgs.add("PARM");
        hideMsgs.add("FMT");
        hideMsgs.add("TIME");
        hideMsgs.add("VER");

        formatNames.put("b", "int8");
        formatNames.put("B", "uint8");
        formatNames.put("L", "int32 * 1e-7 (lat/lon)");
        formatNames.put("i", "int32");
        formatNames.put("I", "uint32");
        formatNames.put("q", "int64");
        formatNames.put("Q", "uint64");
        formatNames.put("f", "float");
        formatNames.put("c", "int16 * 1e-2");
        formatNames.put("C", "uint16 * 1e-2");
        formatNames.put("e", "int32 * 1e-2");
        formatNames.put("E", "uint32 * 1e-2");
        formatNames.put("n", "char[4]");
        formatNames.put("N", "char[16]");
        formatNames.put("Z", "char[64]");
        formatNames.put("M", "uint8 (mode)");
    }

    public PX4LogReader(String fileName) throws IOException, FormatErrorException {
        super(fileName);
        readFormats();
        updateStatistics();
    }

    @Override
    public String getFormat() {
        return formatPX4 ? "PX4" : "APM";
    }

    @Override
    public String getSystemName() {
        return getFormat();
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
        seek(0);
        long packetsNum = 0;
        long timeStart = -1;
        long timeEnd = -1;
        boolean parseVersion = true;
        StringBuilder versionStr = new StringBuilder();
        while (true) {
            PX4LogMessage msg;
            try {
                msg = readMessage();
            } catch (EOFException e) {
                break;
            }
            // Time range
            if (formatPX4) {
                if ("TIME".equals(msg.description.name)) {
                    long t = msg.getLong(0);
                    time = t;
                    if (timeStart < 0) {
                        timeStart = t;
                    }
                    timeEnd = t;
                }
            } else {
                long t = getAPMTimestamp(msg);
                if (t > 0) {
                    if (timeStart < 0) {
                        timeStart = t;
                    }
                    timeEnd = t;
                }
            }
            packetsNum++;

            // Version
            if (formatPX4) {
                if ("VER".equals(msg.description.name)) {
                    String fw = (String) msg.get("FwGit");
                    if (fw != null) {
                        version.put("FW", fw);
                    }
                    String hw = (String) msg.get("Arch");
                    if (hw != null) {
                        version.put("HW", hw);
                    }
                }
            } else {
                if ("MSG".equals(msg.description.name)) {
                    String s = (String) msg.get("Message");
                    if (parseVersion && (s.startsWith("Ardu") || s.startsWith("PX4"))) {
                        if (versionStr.length() > 0) {
                            versionStr.append("; ");
                        }
                        versionStr.append(s);
                    } else {
                        parseVersion = false;
                    }
                }
            }

            // Parameters
            if ("PARM".equals(msg.description.name)) {
                parameters.put((String) msg.get("Name"), msg.get("Value"));
            }

            if ("GPS".equals(msg.description.name)) {
                if (utcTimeReference < 0) {
                    try {
                        if (formatPX4) {
                            int fix = ((Number) msg.get("Fix")).intValue();
                            long gpsT = ((Number) msg.get("GPSTime")).longValue();
                            if (fix >= 3 && gpsT > 0) {
                                utcTimeReference = gpsT - timeEnd;
                            }
                        } else {
                            int fix = ((Number) msg.get("Status")).intValue();
                            int week = ((Number) msg.get("Week")).intValue();
                            long ms = ((Number) msg.get(tsName)).longValue();
                            if (tsMicros) {
                                ms = ms / 1000;
                            }
                            if (fix >= 3 && (week > 0 || ms > 0)) {
                                long leapSeconds = 16;
                                long gpsT = ((315964800L + week * 7L * 24L * 3600L - leapSeconds) * 1000 + ms) * 1000L;
                                utcTimeReference = gpsT - timeEnd;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        startMicroseconds = timeStart;
        sizeUpdates = packetsNum;
        sizeMicroseconds = timeEnd - timeStart;
        if (!formatPX4) {
            version.put("FW", versionStr.toString());
        }
        seek(0);
    }

    @Override
    public boolean seek(long seekTime) throws IOException {
        position(dataStart);
        lastMsg = null;
        if (seekTime == 0) {      // Seek to start of log
            time = 0;
            return true;
        }
        // Seek to specified timestamp without parsing all messages
        try {
            while (true) {
                long pos = position();
                int msgType = readHeader();
                PX4LogMessageDescription messageDescription = messageDescriptions.get(msgType);
                if (messageDescription == null) {
                    errors.add(new FormatErrorException(pos, "Unknown message type: " + msgType));
                    continue;
                }
                int bodyLen = messageDescription.length - HEADER_LEN;
                try {
                    fillBuffer(bodyLen);
                } catch (EOFException e) {
                    errors.add(new FormatErrorException(pos, "Unexpected end of file"));
                    return false;
                }
                if (formatPX4) {
                    if ("TIME".equals(messageDescription.name)) {
                        PX4LogMessage msg = messageDescription.parseMessage(buffer);
                        long t = msg.getLong(0);
                        if (t > seekTime) {
                            // Time found
                            time = t;
                            position(pos);
                            return true;
                        }
                    } else {
                        // Skip the message
                        buffer.position(buffer.position() + bodyLen);
                    }
                } else {
                    Integer idx = messageDescription.fieldsMap.get(tsName);
                    if (idx != null && idx == 0) {
                        PX4LogMessage msg = messageDescription.parseMessage(buffer);
                        long t = msg.getLong(idx);
                        if (!tsMicros) {
                            t *= 1000;
                        }
                        if (t > seekTime) {
                            // Time found
                            time = t;
                            position(pos);
                            return true;
                        }
                    } else {
                        // Skip the message
                        buffer.position(buffer.position() + bodyLen);
                    }
                }
            }
        } catch (EOFException e) {
            return false;
        }
    }

    // return ts in micros
    private long getAPMTimestamp(PX4LogMessage msg) {
        if (null == tsName) {
            // detect APM's timestamp format on first timestamp seen
            if (null != msg.description.fieldsMap.get("TimeUS")) {
                // new format, timestamps in micros
                tsMicros = true;
                tsName = "TimeUS";
            } else if (null != msg.description.fieldsMap.get("TimeMS")) {
                // old format, timestamps in millis
                tsMicros = false;
                tsName = "TimeMS";
            } else {
                return 0;
            }
        }

        Integer idx = msg.description.fieldsMap.get(tsName);
        if (idx != null && idx == 0) {
            return tsMicros ? msg.getLong(idx) : (msg.getLong(idx) * 1000);
        }
        return 0;
    }

    private void applyMsg(Map<String, Object> update, PX4LogMessage msg) {
        String[] fields = msg.description.fields;
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (!formatPX4) {
                if (i == 0 && tsName.equals(field)) {
                    continue;   // Don't apply timestamp field
                }
            }
            update.put(msg.description.name + "." + field, msg.get(i));
        }
    }

    @Override
    public long readUpdate(Map<String, Object> update) throws IOException, FormatErrorException {
        long t = time;
        if (lastMsg != null) {
            applyMsg(update, lastMsg);
            lastMsg = null;
        }
        while (true) {
            PX4LogMessage msg = readMessage();
            if (null == msg) {
                continue;
            }

            if (formatPX4) {
                // PX4 log has TIME message
                if ("TIME".equals(msg.description.name)) {
                    time = msg.getLong(0);
                    if (t == 0) {
                        // The first TIME message
                        t = time;
                        continue;
                    }
                    break;
                }
            } else {
                // APM log doesn't have TIME message
                long ts = getAPMTimestamp(msg);
                if (ts > 0) {
                    time = ts;
                    if (t == 0) {
                        // The first message with timestamp
                        t = time;
                    } else {
                        if (time > t) {
                            // Timestamp changed, leave the message for future
                            lastMsg = msg;
                            break;
                        }
                    }
                }
            }

            applyMsg(update, msg);
        }
        return t;
    }

    @Override
    public Map<String, String> getFields() {
        return fieldsList;
    }

    private void readFormats() throws IOException, FormatErrorException {
        fieldsList = new HashMap<String, String>();
        try {
            while (true) {
                if (fillBuffer() < 0) {
                    break;
                }
                while (true) {
                    if (buffer.remaining() < PX4LogMessageDescription.FORMAT.length) {
                        break;
                    }
                    buffer.mark();
                    int msgType = readHeader();     // Don't try to handle errors in formats
                    if (msgType == PX4LogMessageDescription.FORMAT.type) {
                        // Message description
                        PX4LogMessageDescription msgDescr = new PX4LogMessageDescription(buffer);
                        messageDescriptions.put(msgDescr.type, msgDescr);
                        if ("TIME".equals(msgDescr.name)) {
                            formatPX4 = true;
                        }
                        if (!hideMsgs.contains(msgDescr.name)) {
                            for (int i = 0; i < msgDescr.fields.length; i++) {
                                String field = msgDescr.fields[i];
                                String format = formatNames.get(Character.toString(msgDescr.format.charAt(i)));
                                if (i != 0 || !("TimeMS".equals(field) || "TimeUS".equals(field))) {
                                    fieldsList.put(msgDescr.name + "." + field, format);
                                }
                            }
                        }
                    } else {
                        // Data message
                        if (formatPX4) {
                            // If it's PX4 log then all formats are read
                            buffer.reset();
                            dataStart = position();
                            return;
                        } else {
                            // APM may have format messages in the middle of log
                            // Skip the message
                            PX4LogMessageDescription messageDescription = messageDescriptions.get(msgType);
                            if (messageDescription == null) {
                                buffer.reset();
                                throw new RuntimeException("Unknown message type: " + msgType);
                            }
                            int bodyLen = messageDescription.length - HEADER_LEN;
                            buffer.position(buffer.position() + bodyLen);
                        }
                    }
                }
            }
        } catch (EOFException ignored) {
        }
    }

    private int readHeader() throws IOException {
        long syncErr = -1;
        while (true) {
            fillBuffer(3);
            int p = buffer.position();
            if (buffer.get() != HEADER_HEAD1 || buffer.get() != HEADER_HEAD2) {
                buffer.position(p + 1);
                if (syncErr < 0) {
                    syncErr = position() - 1;
                }
                continue;
            }
            if (syncErr >= 0) {
                errors.add(new FormatErrorException(syncErr, "Bad message header"));
            }
            return buffer.get() & 0xFF;
        }
    }

    /**
     * Read next message from log
     *
     * @return log message
     * @throws IOException  on IO error
     * @throws EOFException on end of stream
     */
    public PX4LogMessage readMessage() throws IOException {
        while (true) {
            int msgType = readHeader();
            long pos = position();
            PX4LogMessageDescription messageDescription = messageDescriptions.get(msgType);
            if (messageDescription == null) {
                errors.add(new FormatErrorException(pos, "Unknown message type: " + msgType));
                continue;
            }
            try {
                fillBuffer(messageDescription.length - HEADER_LEN);
            } catch (EOFException e) {
                errors.add(new FormatErrorException(pos, "Unexpected end of file"));
                throw e;
            }
            return messageDescription.parseMessage(buffer);
        }
    }

    @Override
    public List<Exception> getErrors() {
        return errors;
    }

    public void clearErrors() {
        errors.clear();
    }

    public static void main(String[] args) throws Exception {
        PX4LogReader reader = new PX4LogReader("test.bin");
        long tStart = System.currentTimeMillis();
        while (true) {
            try {
                PX4LogMessage msg = reader.readMessage();
            } catch (EOFException e) {
                break;
            }
        }
        long tEnd = System.currentTimeMillis();
        System.out.println(tEnd - tStart);
        for (Exception e : reader.getErrors()) {
            System.out.println(e.getMessage());
        }
        reader.close();
    }
}
