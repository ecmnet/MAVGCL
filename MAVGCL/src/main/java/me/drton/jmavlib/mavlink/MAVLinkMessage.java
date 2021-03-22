package me.drton.jmavlib.mavlink;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkMessage {
    public final static byte START_OF_FRAME_MAVLINK1 = (byte) 0xFE;
    public final static byte START_OF_FRAME_MAVLINK2 = (byte) 0xFD;
    public final static int MAVLINK1_HEADER_LENGTH = 6;
    public final static int CRC_LENGTH = 2;
    public final static int SIGNATURE_LENGTH = 13;
    public final static int MAVLINK2_MSGID_EXTENSION = 2;
    public final static int MAVLINK2_compatFlags_EXTENSION = 2;
    public final static byte MAVLINK_IFLAG_SIGNED = 0x01;
    private final MAVLinkSchema schema;
    public final MAVLinkMessageDefinition definition;
    public final int msgID;
    private final byte[] payload;
    private final ByteBuffer payloadBB;
    private byte sequence = 0;
    private byte compatFlags = 0; // mavlink 2 only
    private byte incompatFlags = 0; // mavlink 2 only
    public final int systemID;
    public final int componentID;
    private int crc = -1;
    private Charset charset = Charset.forName("latin1");
    public int protocolVersion = 1;
    private boolean signingEnabled = false; // not fully supported, this is only for msg length

    /**
     * Create empty message by message ID (for filling and sending)
     */
    public MAVLinkMessage(MAVLinkSchema schema, int msgID, int systemID, int componentID,
                          int protocolVersion) {
        this.schema = schema;
        this.definition = schema.getMessageDefinition(msgID);
        if (definition == null) {
            throw new RuntimeException("Unknown mavlink message ID: " + msgID);
        }
        this.payload = new byte[definition.payloadLength];
        this.payloadBB = ByteBuffer.wrap(payload);
        payloadBB.order(schema.getByteOrder());
        this.systemID = systemID;
        this.componentID = componentID;
        this.msgID = msgID;
        this.protocolVersion = protocolVersion;
    }

    /**
     * Create empty message by message name (for filling and sending)
     */
    public MAVLinkMessage(MAVLinkSchema schema, String msgName, int systemID, int componentID,
                          int protocolVersion) {
        this.schema = schema;
        this.definition = schema.getMessageDefinition(msgName);
        if (definition == null) {
            throw new RuntimeException("Unknown mavlink message name: " + msgName);
        }
        this.payload = new byte[definition.payloadLength];
        this.payloadBB = ByteBuffer.wrap(payload);
        payloadBB.order(schema.getByteOrder());
        this.systemID = systemID;
        this.componentID = componentID;
        this.msgID = definition.id;
        this.protocolVersion = protocolVersion;
    }

    /**
     * Create message from buffer (for parsing)
     */
    public MAVLinkMessage(MAVLinkSchema schema, ByteBuffer buffer)
    throws MAVLinkProtocolException, MAVLinkUnknownMessage, BufferUnderflowException {
        if (buffer.remaining() < getNonPayloadLength()) {
            throw new BufferUnderflowException();
        }
        int startPos = buffer.position();

        byte startSign = buffer.get();
        if (startSign == START_OF_FRAME_MAVLINK1) {
            protocolVersion = 1;
        } else if (startSign == START_OF_FRAME_MAVLINK2) {
            protocolVersion = 2;
        } else {
            throw new MAVLinkProtocolException(
                String.format("Invalid start sign: %02x, should be %02x (mavlink 1) or %02x (mavlink 2)",
                              startSign, START_OF_FRAME_MAVLINK1, START_OF_FRAME_MAVLINK2));
        }
        int payloadLen = buffer.get() & 0xff;
        if (buffer.remaining() < payloadLen + getNonPayloadLength() - 2) { // 2 bytes was read already
            buffer.position(startPos);
            throw new BufferUnderflowException();
        }

        if (protocolVersion == 2) {
            incompatFlags = buffer.get();
            if ((incompatFlags & MAVLINK_IFLAG_SIGNED) > 0) {
                signingEnabled = true;
            }
            compatFlags = buffer.get();
        }

        sequence = buffer.get();
        systemID = buffer.get() & 0xff;
        componentID = buffer.get() & 0xff;
        if (protocolVersion == 2) {
            // The message ID for mavlink 2 is 3 bytes long.
            msgID = ((buffer.get() & 0xff)) | ((buffer.get() & 0xff) << 8) | ((buffer.get() & 0xff) << 16);
        } else {
            msgID = buffer.get() & 0xff;
        }

        this.schema = schema;
        this.definition = schema.getMessageDefinition(msgID);
        if (definition == null) {
            // Unknown message skip it
            if (protocolVersion == 2) {
                buffer.position(buffer.position() + payloadLen + CRC_LENGTH + getSignatureLength());
            } else {
                buffer.position(buffer.position() + payloadLen + CRC_LENGTH);
            }
            throw new MAVLinkUnknownMessage(String.format("Unknown message: %s", msgID));
        }
        if (protocolVersion == 2 &&
                (payloadLen > definition.payloadLength)) {
            buffer.position(buffer.position() + payloadLen + CRC_LENGTH + getSignatureLength());
            throw new MAVLinkUnknownMessage(
                String.format("Invalid payload len for msg %s (%s): %s, should be %s to %s", definition.name, msgID,
                              payloadLen, definition.payloadMinimumLength, definition.payloadLength));
        } else if (protocolVersion == 1 &&
                   (payloadLen != definition.payloadLength && payloadLen != definition.payloadMinimumLength)) {
            buffer.position(buffer.position() + payloadLen + CRC_LENGTH);
            throw new MAVLinkUnknownMessage(
                String.format("Invalid payload len for msg %s (%s): %s, should be %s or %s", definition.name, msgID,
                              payloadLen, definition.payloadMinimumLength, definition.payloadLength));

        }

        // We take the definition length becuase the payload might be shortened if it is all zeros.
        this.payload = new byte[definition.payloadLength];
        // Only get as much as is set in the buffer.
        buffer.get(payload, 0, payloadLen);
        crc = Short.reverseBytes(buffer.getShort()) & 0xffff;
        int endPos = buffer.position();
        buffer.position(startPos);
        int crcCalc = calculateCRC(buffer, payloadLen);
        buffer.position(endPos);
        if (crc != crcCalc) {
            throw new MAVLinkUnknownMessage(
                String.format("CRC error for msg %s (%d): %04x, should be %04x", definition.name, msgID, crc,
                              crcCalc));
        }
        this.payloadBB = ByteBuffer.wrap(payload);
        payloadBB.order(schema.getByteOrder());

        if (protocolVersion == 2 && signingEnabled) {
            // Remove the signature bytes from the buffer but don't use them yet.
            byte signature[] = new byte[getSignatureLength()];
            buffer.get(signature);
        }
    }

    public ByteBuffer encode(byte sequence) {
        this.sequence = sequence;
        ByteBuffer buf = ByteBuffer.allocate(payload.length + getNonPayloadLength());
        buf.order(schema.getByteOrder());
        if (protocolVersion == 2) {
            buf.put(START_OF_FRAME_MAVLINK2);
        } else {
            buf.put(START_OF_FRAME_MAVLINK1);
        }
        buf.put((byte) payload.length);
        if (protocolVersion == 2) {
            buf.put((byte) 0); // incompatFlags
            buf.put((byte) 0); // compatFlags
        }
        buf.put(sequence);
        buf.put((byte) systemID);
        buf.put((byte) componentID);
        if (protocolVersion == 2) {
            buf.put((byte)((msgID & 0x0000FF)));
            buf.put((byte)((msgID & 0x00FF00) >> 8));
            buf.put((byte)((msgID & 0xFF0000) >> 16));
        } else {
            buf.put((byte) msgID);
        }
        buf.put(payload);
        buf.flip();
        crc = calculateCRC(buf, payload.length);
        buf.limit(buf.capacity());
        buf.put((byte) crc);
        buf.put((byte)(crc >> 8));
        buf.flip();
        return buf;
    }

    /**
     * Calculate CRC of the message, buffer position should be set to start of the message.
     *
     * @param buf
     * @return CRC
     */
    private int calculateCRC(ByteBuffer buf, int payloadLen) {
        buf.get();  // Skip start sign
        int c = 0xFFFF;
        for (int i = 0; i < payloadLen + getHeaderLength() - 1; i++) {
            c = MAVLinkCRC.accumulateCRC(buf.get(), c);
        }
        c = MAVLinkCRC.accumulateCRC(definition.extraCRC, c);
        return c;
    }

    private int getHeaderLength() {
        if (protocolVersion == 2) {
            return MAVLINK1_HEADER_LENGTH + MAVLINK2_MSGID_EXTENSION + MAVLINK2_compatFlags_EXTENSION;
        } else {
            return MAVLINK1_HEADER_LENGTH;
        }
    }

    private int getNonPayloadLength() {
        return getHeaderLength() + CRC_LENGTH;
    }

    private int getSignatureLength() {
        if (signingEnabled) {
            return SIGNATURE_LENGTH;
        } else {
            return 0;
        }
    }

    public int getMsgType() {
        return definition.id;
    }

    public String getMsgName() {
        return definition.name;
    }

    public Object get(MAVLinkField field) {
        if (field.isArray()) {
            if (field.type == MAVLinkDataType.CHAR) {
                // Char array (string)
                byte[] buf = new byte[field.arraySize];
                payloadBB.position(field.offset);
                payloadBB.get(buf);
                // Find NULL terminating char
                int n = 0;
                while (n < buf.length && buf[n] != 0) {
                    n++;
                }
                return new String(buf, 0, n, charset);
            } else if (field.type == MAVLinkDataType.UINT8) {
                // Byte array
                byte[] buf = new byte[field.arraySize];
                payloadBB.position(field.offset);
                payloadBB.get(buf);
                return buf;
            } else {
                Object[] res = new Object[field.arraySize];
                int offs = field.offset;
                for (int i = 0; i < field.arraySize; i++) {
                    res[i] = getValue(field.type, offs);
                    offs += field.type.size;
                }
                return res;
            }
        } else {
            return getValue(field.type, field.offset);
        }
    }

    private Object getValue(MAVLinkDataType type, int offset) {
        switch (type) {
            case CHAR:
                return payloadBB.get(offset);
            case UINT8:
                return payloadBB.get(offset) & 0xFF;
            case INT8:
                return (int) payloadBB.get(offset);
            case UINT16:
                return payloadBB.getShort(offset) & 0xFFFF;
            case INT16:
                return (int) payloadBB.getShort(offset);
            case UINT32:
                return payloadBB.getInt(offset) & 0xFFFFFFFFl;
            case INT32:
                return payloadBB.getInt(offset);
            case UINT64:
                return payloadBB.getLong(offset);
            case INT64:
                return payloadBB.getLong(offset);
            case FLOAT:
                return payloadBB.getFloat(offset);
            case DOUBLE:
                return payloadBB.getDouble(offset);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    public void set(MAVLinkField field, Object value) {
        if (field.isArray()) {
            Object[] valueArray;
            if (value instanceof String) {
                // String (array of chars)
                String valueStr = (String) value;
                valueArray = new Byte[field.arraySize];
                for (int i = 0; i < field.arraySize; i++) {
                    payloadBB.put(field.offset + i, i < valueStr.length() ? (byte) valueStr.charAt(i) : 0);
                }
            } else if (value instanceof byte[]) {
                // Array of bytes, binary data
                byte[] valueBytes = (byte[]) value;
                for (int i = 0; i < field.arraySize; i++) {
                    payloadBB.put(field.offset + i, i < valueBytes.length ? valueBytes[i] : 0);
                }
            } else {
                // Generic array
                valueArray = (Object[]) value;
                int offset = field.offset;
                for (int i = 0; i < field.arraySize; i++) {
                    setValue(field.type, offset, valueArray[i]);
                    offset += field.type.size;
                }
            }
        } else {
            // Single value
            setValue(field.type, field.offset, value);
        }
    }

    private void setValue(MAVLinkDataType type, int offset, Object value) {
        switch (type) {
            case CHAR:
            case UINT8:
            case INT8:
                payloadBB.put(offset, ((Number) value).byteValue());
                break;
            case UINT16:
            case INT16:
                payloadBB.putShort(offset, ((Number) value).shortValue());
                break;
            case UINT32:
            case INT32:
                payloadBB.putInt(offset, ((Number) value).intValue());
                break;
            case UINT64:
            case INT64:
                payloadBB.putLong(offset, ((Number) value).longValue());
                break;
            case FLOAT:
                payloadBB.putFloat(offset, ((Number) value).floatValue());
                break;
            case DOUBLE:
                payloadBB.putDouble(offset, ((Number) value).doubleValue());
                break;
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    public Object get(String fieldName) {
        return get(definition.fieldsByName.get(fieldName));
    }

    public void set(String fieldName, Object value) {
        set(definition.fieldsByName.get(fieldName), value);
    }

    public Object get(int fieldID) {
        return get(definition.fields[fieldID]);
    }

    public void set(int fieldID, Object value) {
        set(definition.fields[fieldID], value);
    }

    public int getInt(String fieldName) {
        return ((Number) get(fieldName)).intValue();
    }

    public int getInt(int fieldID) {
        return ((Number) get(fieldID)).intValue();
    }

    public long getLong(String fieldName) {
        return ((Number) get(fieldName)).longValue();
    }

    public long getLong(int fieldID) {
        return ((Number) get(fieldID)).longValue();
    }

    public float getFloat(String fieldName) {
        return ((Number) get(fieldName)).floatValue();
    }

    public float getFloat(int fieldID) {
        return ((Number) get(fieldID)).floatValue();
    }

    public double getDouble(String fieldName) {
        return ((Number) get(fieldName)).doubleValue();
    }

    public double getDouble(int fieldID) {
        return ((Number) get(fieldID)).doubleValue();
    }

    public String getString(int fieldID) {
        return (String) get(fieldID);
    }

    public String getString(String fieldName) {
        return (String) get(fieldName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MAVLinkField field : definition.fields) {
            sb.append(field.name);
            sb.append("=");
            sb.append(get(field));
            sb.append(" ");
        }
        return String.format("<MAVLinkMessage %s seq=%s sysID=%s compID=%s ID=%s CRC=%04x %s/>",
                             definition.name,
                             sequence & 0xff, systemID, componentID, msgID, crc, sb.toString());
    }
}
