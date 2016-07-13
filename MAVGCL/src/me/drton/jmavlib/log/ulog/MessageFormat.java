package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * User: ton Date: 03.06.13 Time: 14:35
 */
public class MessageFormat {
    public static Charset charset = Charset.forName("latin1");
    public final String name;
    public final FieldFormat[] fields;
    public final Map<String, Integer> fieldsMap = new HashMap<String, Integer>();

    public static String getString(ByteBuffer buffer, int len) {
        byte[] strBuf = new byte[len];
        buffer.get(strBuf);
        String[] p = new String(strBuf, charset).split("\0");
        return p.length > 0 ? p[0] : "";
    }

    public MessageFormat(ByteBuffer buffer, int msgSize) {


        String[] descr_str = getString(buffer, msgSize).split(":");
        name = descr_str[0];
        if (descr_str.length > 1) {
            String[] fields_descrs_str = descr_str[1].split(";");
            fields = new FieldFormat[fields_descrs_str.length];
            for (int i = 0; i < fields_descrs_str.length; i++) {
                String field_format_str = fields_descrs_str[i];
                fields[i] = new FieldFormat(field_format_str);
                if(i==(fields_descrs_str.length-1) && fields[i].name.startsWith("_p"))
                	break;
                fieldsMap.put(fields[i].name, i);
            }
        } else {
            fields = new FieldFormat[0];
        }

    }

    public List<Object> parseBody(ByteBuffer buffer) {
        List<Object> data = new ArrayList<Object>(fields.length);
        for (FieldFormat field : fields) {
        	if(!field.name.startsWith("_p"))
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
        return String.format("FORMAT: name=%s, fields=%s", name, Arrays.asList(fields));
    }
}
