package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * User: ton Date: 03.06.13 Time: 14:35
 */
public class MessageFormat {
    public static Charset charset = Charset.forName("latin1");
    public final int msgID;
    public final String name;
    public final FieldFormat[] fields;
    public final Map<String, Integer> fieldsMap = new HashMap<String, Integer>();

    public static String getString(ByteBuffer buffer, int len) {
        byte[] strBuf = new byte[len];
        buffer.get(strBuf);
        String[] p = new String(strBuf, charset).split("\0");
        return p.length > 0 ? p[0] : "";
    }

    public MessageFormat(ByteBuffer buffer, int logVersion) {
        msgID = buffer.get() & 0xFF;
        int format_len;
        if (logVersion == 0) {
            format_len = buffer.get() & 0xFF;
        } else {
            format_len = buffer.getShort() & 0xFFFF;
        }
        String[] descr_str = getString(buffer, format_len).split(":");
        name = descr_str[0];
        if (descr_str.length > 1) {
            String[] fields_descrs_str = descr_str[1].split(";");
            fields = new FieldFormat[fields_descrs_str.length];
            for (int i = 0; i < fields_descrs_str.length; i++) {
                String field_format_str = fields_descrs_str[i];
                fields[i] = new FieldFormat(field_format_str);
                fieldsMap.put(fields[i].name, i);
            }
        } else {
            fields = new FieldFormat[0];
        }
    }

    public List<Object> parseBody(ByteBuffer buffer) {
        List<Object> data = new ArrayList<Object>(fields.length);
        for (FieldFormat field : fields) {
            data.add(field.getValue(buffer));
        }
        return data;
    }

    public List<String> getFields() {
        List<String> field_names = new ArrayList<String>(fields.length);
        for (FieldFormat field : fields) {
            field_names.add(field.name);
        }
        return field_names;
    }

    @Override
    public String toString() {
        return String.format("FORMAT: type=%s, name=%s, fields=%s",
                msgID, name, Arrays.asList(fields));
    }
}
