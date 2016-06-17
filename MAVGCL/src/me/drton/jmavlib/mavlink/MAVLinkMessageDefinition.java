package me.drton.jmavlib.mavlink;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 03.06.14 Time: 12:33
 */
public class MAVLinkMessageDefinition {
    public final int id;
    public final String name;
    public final byte extraCRC;
    public final Map<String, MAVLinkField> fieldsByName;
    public final MAVLinkField[] fields;
    public final int payloadLength;

    public MAVLinkMessageDefinition(int id, String name, MAVLinkField[] fields) {
        this.id = id;
        this.name = name;
        this.fields = fields;
        this.fieldsByName = new HashMap<String, MAVLinkField>(fields.length);
        int len = 0;
        for (MAVLinkField field : fields) {
            fieldsByName.put(field.name, field);
            field.offset = len;
            len += field.size;
        }
        this.payloadLength = len;
        this.extraCRC = calculateExtraCRC();
    }

    private byte calculateExtraCRC() {
        String extraCRCStr = name + " ";
        for (MAVLinkField field : fields) {
            extraCRCStr += field.type.ctype + " " + field.name + " ";
            if (field.isArray()) {
                extraCRCStr += (char) field.arraySize;
            }
        }
        int extraCRCRaw = MAVLinkCRC.calculateCRC(extraCRCStr.getBytes(Charset.forName("latin1")));
        return (byte) ((extraCRCRaw & 0x00FF) ^ ((extraCRCRaw >> 8 & 0x00FF)));
    }
}
