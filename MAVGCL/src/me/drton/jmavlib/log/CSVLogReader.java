package me.drton.jmavlib.log;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ton Date: 10.06.14 Time: 12:46
 */
public class CSVLogReader implements LogReader {
    private RandomAccessFile file;
    private String[] fields;
    private Map<String, String> fieldsFormats;
    private String delimiter = ";";
    private int columnTime = 0;
    private long sizeUpdates = -1;
    private long sizeMicroseconds = -1;
    private long startMicroseconds = -1;

    public CSVLogReader(String fileName) throws IOException, FormatErrorException {
        file = new RandomAccessFile(fileName, "r");
        readFormats();
        updateStatistics();
    }

    private void readFormats() throws IOException, FormatErrorException {
        String headerLine = file.readLine();
        if (headerLine == null) {
            throw new FormatErrorException("Empty CSV file");
        }
        fields = headerLine.split(delimiter);
        fieldsFormats = new HashMap<String, String>(fields.length);
        for (String field : fields) {
            fieldsFormats.put(field, "d");
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    @Override
    public boolean seek(long seekTime) throws FormatErrorException, IOException {
        if (seekTime == 0) {
            file.seek(0);
            file.readLine();
            return true;
        }
        long t = 0;
        Map<String, Object> data = new HashMap<String, Object>();
        while (t < seekTime) {
            data.clear();
            long ptr = file.getFilePointer();
            try {
                t = readUpdate(data);
            } catch (EOFException e) {
                return false;
            }
            if (t > seekTime) {
                file.seek(ptr);
                return true;
            }
        }
        return false;
    }

    private void updateStatistics() throws IOException, FormatErrorException {
        seek(0);
        long packetsNum = 0;
        long timeStart = -1;
        long timeEnd = -1;
        while (true) {
            String[] values;
            try {
                values = readLineValues();
            } catch (EOFException e) {
                break;
            }

            if (values.length > columnTime) {
                double v = Double.parseDouble(values[columnTime].replace(',', '.'));
                long t = (long) (v * 1000000);
                if (timeStart < 0) {
                    timeStart = t;
                }
                timeEnd = t;
                packetsNum++;
            }
        }
        startMicroseconds = timeStart;
        sizeUpdates = packetsNum;
        sizeMicroseconds = timeEnd - timeStart;
        seek(0);
    }

    @Override
    public long readUpdate(Map<String, Object> update) throws IOException, FormatErrorException {
        String[] values = readLineValues();
        long t = 0;
        for (int i = 0; i < values.length; i++) {
            if (i < fields.length && !fields[i].isEmpty()) {
                double v = Double.parseDouble(values[i].replace(',', '.'));
                if (i == columnTime) {
                    t = (long) (v * 1000000);
                } else {
                    update.put(fields[i], v);
                }
            }
        }
        return t;
    }

    private String[] readLineValues() throws IOException {
        String line = file.readLine();
        if (line == null) {
            throw new EOFException();
        }
        return line.split(delimiter);
    }

    @Override
    public Map<String, String> getFields() {
        return fieldsFormats;
    }

    @Override
    public String getFormat() {
        return "CSV";
    }

    @Override
    public String getSystemName() {
        return "";
    }

    @Override
    public long getSizeUpdates() {
        return sizeUpdates;
    }

    @Override
    public long getStartMicroseconds() {
        return startMicroseconds;
    }

    @Override
    public long getSizeMicroseconds() {
        return sizeMicroseconds;
    }

    @Override
    public long getUTCTimeReferenceMicroseconds() {
        return -1;  // Not supported
    }

    @Override
    public Map<String, Object> getVersion() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getParameters() {
        return null;
    }

    @Override
    public List<Exception> getErrors() {
        return Collections.emptyList();
    }

    @Override
    public void clearErrors() {
    }

    public static void main(String[] args) throws Exception {
        CSVLogReader reader = new CSVLogReader("test.csv");
        long tStart = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<String, Object>();
        while (true) {
            long t;
            data.clear();
            try {
                t = reader.readUpdate(data);
            } catch (EOFException e) {
                break;
            }
            System.out.println(t + " " + data);
        }
        long tEnd = System.currentTimeMillis();
        System.out.println(tEnd - tStart);
        reader.close();
    }
}
