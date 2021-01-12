package me.drton.jmavlib.mavlink;

/**
 * User: ton Date: 03.06.14 Time: 15:35
 */
public class MAVLinkCRC {
    public final static int X25_INIT_CRC = 0xffff;

    /**
     * Accumulate the X.25 CRC by adding one char at a time. The checksum function adds the hash of one char at a time
     * to the 16 bit checksum
     *
     * @param data new char to hash
     * @param crc  the already accumulated checksum
     * @return the new accumulated checksum
     */
    public static int accumulateCRC(byte data, int crc) {
        int tmp = (data ^ crc) & 0xff;
        tmp ^= (tmp << 4) & 0xff;
        return ((crc >> 8) ^ (tmp << 8) ^ (tmp << 3) ^ (tmp >> 4)) & 0xffff;
    }

    public static int calculateCRC(byte[] data) {
        int crc = X25_INIT_CRC;
        for (byte b : data) {
            crc = accumulateCRC(b, crc);
        }
        return crc;
    }
}
