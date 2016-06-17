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

    /**
     * MAVLink field constructor.
     * @param type field type
     * @param arraySize number of elements in array (-1 if it's not an array)
     * @param name field name
     */
    public MAVLinkField(MAVLinkDataType type, int arraySize, String name) {
        this.name = name;
        this.type = type;
        if (arraySize >= 0) {
            this.size = type.size * arraySize;
        } else {
            this.size = type.size;
        }
        this.arraySize = arraySize;
    }

    /**
     * Check if field contains array.
     * @return true for array field, false for single value field
     */
    public boolean isArray() {
        return arraySize >= 0;  // Array of size 0 or 1 is not very useful, but still possible
    }
}
