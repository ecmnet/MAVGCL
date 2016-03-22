package me.drton.jmavlib.log;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * User: ton Date: 03.06.13 Time: 17:45
 */
public interface LogReader {
    void close() throws IOException;

    /**
     * Seek to specified time.
     *
     * @param time time to seek in us, 0 for start of file
     * @return true if time found or false if last timestamp in the log is less then specified
     * @throws IOException
     * @throws FormatErrorException
     */
    boolean seek(long time) throws IOException, FormatErrorException;

    /**
     * Read next update from the log and put it to 'update' map, unused keys will be not deleted.
     *
     * @param update map to store update
     * @return time of update in us
     * @throws IOException
     * @throws FormatErrorException
     */
    long readUpdate(Map<String, Object> update) throws IOException, FormatErrorException;

    /**
     * Get map of field - format.
     *
     * @return map of fields - formats
     */
    Map<String, String> getFields();

    /**
     * Get log format.
     *
     * @return log format string
     */
    String getFormat();

    /**
     * Get system name.
     */
    String getSystemName();

    /**
     * Get number of records (updates) in the log.
     *
     * @return number of records
     */
    long getSizeUpdates();

    /**
     * Get timestamp of the first record in the log.
     *
     * @return timestamp in us of the first record
     */
    long getStartMicroseconds();

    /**
     * Get log size in us.
     *
     * @return last timestamp - first timestamp
     */
    long getSizeMicroseconds();

    /**
     * Get UTC time reference as Unix time, i.e. UTC time of time 0 in the log.
     *
     * @return UTC time reference in us
     */
    long getUTCTimeReferenceMicroseconds();

    /**
     * Get version of device that recorded the log (optional).
     *
     * @return version map or null if not supported by format
     */
    Map<String, Object> getVersion();

    /**
     * Get parameters of device that recorded the log (optional).
     *
     * @return parameters map or null if not supported
     */
    Map<String, Object> getParameters();

    /**
     * Return list on non-fatal errors happened during log reading.
     *
     * @return list of errors, null not allowed.
     */
    List<Exception> getErrors();

    /**
     * Clear errors list.
     */
    void clearErrors();
}
