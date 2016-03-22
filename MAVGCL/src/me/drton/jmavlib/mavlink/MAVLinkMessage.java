package me.drton.jmavlib.mavlink;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkMessage {
    public final static int HEADER_LENGTH = 6;
    public final static int CRC_LENGTH = 2;
    public final static int NON_PAYLOAD_LENGTH = HEADER_LENGTH + CRC_LENGTH;
    private final MAVLinkSchema schema;
    public final MAVLinkMessageDefinition definition;
    public final int msgID;
    private final byte[] payload;
    private final ByteBuffer payloadBB;
    private byte sequence = 0;
    public final int systemID;
    public final int componentID;
    private int crc = -1;
    private Charset charset = Charset.forName("latin1");

    /**
     * Create empty message by message ID (for filling and sending)
     *
     * @param schema
     * @param msgID
     */
    public MAVLinkMessage(MAVLinkSchema schema, int msgID, int systemID, int componentID) {
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
    }

    /**
     * Create empty message by message name (for filling and sending)
     *
     * @param schema
     * @param msgName
     */
    public MAVLinkMessage(MAVLinkSchema schema, String msgName, int systemID, int componentID) {
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
    }

    /**
     * Create message from buffer (for parsing)
     *
     * @param schema
     */
    public MAVLinkMessage(MAVLinkSchema schema, ByteBuffer buffer)
            throws MAVLinkProtocolException, MAVLinkUnknownMessage, BufferUnderflowException {
        if (buffer.remaining() < NON_PAYLOAD_LENGTH) {
            throw new BufferUnderflowException();
        }
        int startPos = buffer.position();
        byte startSign = buffer.get();
        if (startSign != schema.getStartSign()) {
            throw new MAVLinkProtocolException(
                    String.format("Invalid start sign: %02x, should be %02x", startSign, schema.getStartSign()));
        }
        int payloadLen = buffer.get() & 0xff;
        if (buffer.remaining() < payloadLen + NON_PAYLOAD_LENGTH - 2) { // 2 bytes was read already
            buffer.position(startPos);
            throw new BufferUnderflowException();
        }
        sequence = buffer.get();
        systemID = buffer.get() & 0xff;
        componentID = buffer.get() & 0xff;
        msgID = buffer.get() & 0xff;
        this.schema = schema;
        this.definition = schema.getMessageDefinition(msgID);
        if (definition == null) {
            // Unknown message skip it
            buffer.position(buffer.position() + payloadLen + CRC_LENGTH);
            throw new MAVLinkUnknownMessage(String.format("Unknown message: %s", msgID));
        }
        if (payloadLen != definition.payloadLength) {
            buffer.position(buffer.position() + payloadLen + CRC_LENGTH);
            throw new MAVLinkUnknownMessage(
                    String.format("Invalid payload len for msg %s (%s): %s, should be %s", definition.name, msgID,
                            payloadLen, definition.payloadLength));
        }
        this.payload = new byte[definition.payloadLength];
        buffer.get(payload);
        crc = Short.reverseBytes(buffer.getShort()) & 0xffff;
        int endPos = buffer.position();
        buffer.position(startPos);
        int crcCalc = calculateCRC(buffer);
        buffer.position(endPos);
        if (crc != crcCalc) {
            throw new MAVLinkUnknownMessage(
                    String.format("CRC error for msg %s (%s): %02x, should be %02x", definition.name, msgID, crc,
                            crcCalc));
        }
        this.payloadBB = ByteBuffer.wrap(payload);
        payloadBB.order(schema.getByteOrder());
    }

    public ByteBuffer encode(byte sequence) {
        this.sequence = sequence;
        ByteBuffer buf = ByteBuffer.allocate(payload.length + NON_PAYLOAD_LENGTH);
        buf.order(schema.getByteOrder());
        buf.put(schema.getStartSign());
        buf.put((byte) definition.payloadLength);
        buf.put(sequence);
        buf.put((byte) systemID);
        buf.put((byte) componentID);
        buf.put((byte) msgID);
        buf.put(payload);
        buf.flip();
        crc = calculateCRC(buf);
        buf.limit(buf.capacity());
        buf.put((byte) crc);
        buf.put((byte) (crc >> 8));
        buf.flip();
        return buf;
    }

    /**
     * Calculate CRC of the message, buffer position should be set to start of the message.
     *
     * @param buf
     * @return CRC
     */
    private int calculateCRC(ByteBuffer buf) {
        buf.get();  // Skip start sign
        int c = 0xFFFF;
        for (int i = 0; i < definition.payloadLength + HEADER_LENGTH - 1; i++) {
            c = MAVLinkCRC.accumulateCRC(buf.get(), c);
        }
        c = MAVLinkCRC.accumulateCRC(definition.extraCRC, c);
        return c;
    }

    public int getMsgType() {
        return definition.id;
    }

    public String getMsgName() {
        return definition.name;
    }

    public Object get(MAVLinkField field) {
        if (field.arraySize > 1) {
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
        if (field.arraySize > 1) {
            Object[] valueArray;
            if (value instanceof String) {
                String valueStr = (String) value;
                valueArray = new Byte[field.arraySize];
                for (int i = 0; i < field.arraySize; i++) {
                    valueArray[i] = i < valueStr.length() ? (byte) valueStr.charAt(i) : 0;
                }
            } else {
                valueArray = (Object[]) value;
            }
            int offset = field.offset;
            for (int i = 0; i < field.arraySize; i++) {
                setValue(field.type, offset, valueArray[i]);
                offset += field.type.size;
            }
        } else {
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
        return String.format("<MAVLinkMessage %s seq=%s sysID=%s compID=%s ID=%s CRC=%04x %s/>", definition.name,
                sequence & 0xff, systemID, componentID, msgID, crc, sb.toString());
    }
}
