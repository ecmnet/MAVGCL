package me.drton.jmavlib.log;

/**
 * User: ton Date: 16.06.13 Time: 13:30
 */
public class FormatErrorException extends Exception {
    public FormatErrorException(String s) {
        super(s);
    }

    public FormatErrorException(long position, String s) {
        super(position + ": " + s);
    }

    public FormatErrorException(long position, String s, Throwable cause) {
        super(position + ": " + s, cause);
    }
}
