package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ton Date: 03.06.13 Time: 14:35
 */
public class MessageFormat {
    public static Charset charset = Charset.forName("latin1");
    public final String name;
    public ArrayList<FieldFormat> fields;
    public final Map<String, Integer> fieldsMap = new HashMap<String, Integer>();
    private boolean nestedParsingDone = false;

    /** max multi id of all logged messages with this format */
    public int maxMultiID = 0;

    public static String getString(ByteBuffer buffer, int len) {
        byte[] strBuf = new byte[len];
        buffer.get(strBuf);
        String[] p = new String(strBuf, charset).split("\0");
        return p.length > 0 ? p[0] : "";
    }

    public MessageFormat(ByteBuffer buffer, int formatLen) {
        String[] descr_str = getString(buffer, formatLen).split(":");
        name = descr_str[0];
        if (descr_str.length > 1) {
            String[] fields_descrs_str = descr_str[1].split(";");
            fields = new ArrayList<FieldFormat>(fields_descrs_str.length);
            for (int i = 0; i < fields_descrs_str.length; i++) {
                String field_format_str = fields_descrs_str[i];
                fields.add(new FieldFormat(field_format_str));
                fieldsMap.put(fields.get(i).name, i);
            }
        } else {
            fields = new ArrayList<FieldFormat>();
        }
    }

    public void parseNestedTypes(final Map<String, MessageFormat> messageFormats) {
        if (nestedParsingDone)
            return;
        //we flatten all nested definitions, because the upper layers
        //do not support tree representations, only linear.
        ArrayList<FieldFormat> newFields = new ArrayList<FieldFormat>(fields.size());
        for (int i = 0; i < fields.size(); ++i) {
            //check if it's a nested type. We assume if it's in messageFormats
            //it's not a built-in type. This means someone *could* override
            //a built-in type.
            MessageFormat m = messageFormats.get(fields.get(i).type);
            if (m != null) {
                m.parseNestedTypes(messageFormats);
                String prefix = fields.get(i).name;
                int arraySize = 1;
                if (fields.get(i).isArray())
                    arraySize = fields.get(i).size;
                for (int array = 0; array < arraySize; ++array) {
                    for (int k = 0; k < m.fields.size(); ++k) {
                        FieldFormat field = m.fields.get(k);
                        String arrayStr = "";
                        if (arraySize > 1)
                            arrayStr = "[" + String.valueOf(array) + "]";
                        FieldFormat newFormat = new FieldFormat(prefix + arrayStr + "." + field.name, field.type,
                                field.size);
                        newFields.add(newFormat);
                    }
                }
            } else {
                newFields.add(fields.get(i));
            }
        }

        fields = newFields;

        nestedParsingDone = true;
    }

    public List<Object> parseBody(ByteBuffer buffer) {
        List<Object> data = new ArrayList<Object>(fields.size());
        for (FieldFormat field : fields) {
            data.add(field.getValue(buffer));
        }
        return data;
    }

    public void removeLastPaddingField() {
        if (fields.size() > 0) {
            if (fields.get(fields.size() - 1).name.startsWith("_padding")) {
                fields.remove(fields.size() - 1);
            }
        }
    }

    public List<String> getFields() {
        List<String> field_names = new ArrayList<String>(fields.size());
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
