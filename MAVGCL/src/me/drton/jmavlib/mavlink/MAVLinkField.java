package me.drton.jmavlib.mavlink;

/**
 * User: ton Date: 03.06.14 Time: 12:54
 */
public class MAVLinkField {
    public final String name;
    public final MAVLinkDataType type;
    public int offset;
    public final int size;
    public final int arraySize;

    public MAVLinkField(MAVLinkDataType type, String name, int offset) {
        this(type, 1, name);
    }

    public MAVLinkField(MAVLinkDataType type, int arraySize, String name) {
        this.name = name;
        this.type = type;
        this.size = type.size * arraySize;
        this.arraySize = arraySize;
    }

    public boolean isArray() {
        return arraySize > 1;
    }
}
