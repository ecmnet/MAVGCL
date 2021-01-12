package me.drton.jmavlib.mavlink;

/**
 * User: ton Date: 03.06.14 Time: 12:36
 */
public enum MAVLinkDataType {
    CHAR(0),
    UINT8(1),
    INT8(2),
    UINT16(3),
    INT16(4),
    UINT32(5),
    INT32(6),
    UINT64(7),
    INT64(8),
    FLOAT(9),
    DOUBLE(10);

    public final int id;
    public final int size;
    public final String ctype;

    MAVLinkDataType(int id) {
        this.id = id;

        switch (id) {
            case 0:
                size = 1;
                ctype = "char";
                break;

            case 1:
                size = 1;
                ctype = "uint8_t";
                break;

            case 2:
                size = 1;
                ctype = "int8_t";
                break;

            case 3:
                size = 2;
                ctype = "uint16_t";
                break;

            case 4:
                size = 2;
                ctype = "int16_t";
                break;

            case 5:
                size = 4;
                ctype = "uint32_t";
                break;

            case 6:
                size = 4;
                ctype = "int32_t";
                break;

            case 7:
                size = 8;
                ctype = "uint64_t";
                break;

            case 8:
                size = 8;
                ctype = "int64_t";
                break;

            case 9:
                size = 4;
                ctype = "float";
                break;

            case 10:
                size = 8;
                ctype = "double";
                break;

            default:
                throw new RuntimeException("Unknown type: " + this);
        }
    }

    public static MAVLinkDataType fromCType(String ctype) {
        if ("char".equals(ctype)) {
            return CHAR;
        } else if ("uint8_t_mavlink_version".equals(ctype)) {
            return UINT8;
        } else if ("uint8_t".equals(ctype)) {
            return UINT8;
        } else if ("int8_t".equals(ctype)) {
            return INT8;
        } else if ("uint16_t".equals(ctype)) {
            return UINT16;
        } else if ("int16_t".equals(ctype)) {
            return INT16;
        } else if ("uint32_t".equals(ctype)) {
            return UINT32;
        } else if ("int32_t".equals(ctype)) {
            return INT32;
        } else if ("uint64_t".equals(ctype)) {
            return UINT64;
        } else if ("int64_t".equals(ctype)) {
            return INT16;
        } else if ("float".equals(ctype)) {
            return FLOAT;
        } else if ("double".equals(ctype)) {
            return DOUBLE;
        }
        throw new RuntimeException("Unknown C type: " + ctype);
    }
}
