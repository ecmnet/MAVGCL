package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

/**
 * Created by ton on 26.10.15.
 */
class FieldFormat {
    public final String name;
    public final String type;
    public final int size; //array length (-1 if not an array)

    public FieldFormat(String formatStr) {
        String[] p = formatStr.split(" ");
        name = p[1];
        if (p[0].contains("[")) {
            // Array
            String[] q = p[0].split("\\[");
            type = q[0];
            size = Integer.parseInt(q[1].split("\\]")[0]);
        } else {
            type = p[0];
            size = -1;
        }
    }

    public FieldFormat(String name, String type, int size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }

    public String getFullTypeString() {
        String size_str = (size >= 0) ? ("[" + size + "]") : "";
        return type + size_str;

    }

    boolean isArray() {
        return size >= 0 && !"char".equals(type);
    }

    public Object getValue(ByteBuffer buffer) {
        Object v;
        if (size >= 0) {
            if (type.equals("char")) {
                byte[] stringBytes = new byte[size];
                buffer.get(stringBytes);
                String s = new String(stringBytes);
                int end = s.indexOf('\0');
                if (end < 0) {
                    v = s;
                } else {
                    v = s.substring(0, end);
                }
            } else {
                Object[] arr = new Object[size];
                for (int i = 0; i < size; i++) {
                    arr[i] = getSingleValue(buffer);
                }
                v = arr;
            }
        } else {
            v = getSingleValue(buffer);
        }
        return v;
    }

    private Object getSingleValue(ByteBuffer buffer) {
        Object v;
        if (type.equals("float")) {
            v = buffer.getFloat();
        } else if (type.equals("double")) {
            v = buffer.getDouble();
        } else if (type.equals("int8_t") || type.equals("bool")) {
            v = (int) buffer.get();
        } else if (type.equals("uint8_t")) {
            v = buffer.get() & 0xFF;
        } else if (type.equals("int16_t")) {
            v = (int) buffer.getShort();
        } else if (type.equals("uint16_t")) {
            v = buffer.getShort() & 0xFFFF;
        } else if (type.equals("int32_t")) {
            v = buffer.getInt();
        } else if (type.equals("uint32_t")) {
            v = buffer.getInt() & 0xFFFFFFFFl;
        } else if (type.equals("int64_t")) {
            v = buffer.getLong();
        } else if (type.equals("uint64_t")) {
            v = buffer.getLong();
        } else if (type.equals("char")) {
            v = buffer.get();
        } else {
            throw new RuntimeException("Unsupported type: " + type);
        }
        return v;
    }

    public String toString() {
        return String.format("%s %s", getFullTypeString(), name);
    }
}
