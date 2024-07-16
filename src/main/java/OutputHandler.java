import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * The OutputHandler class provides methods to write different types of responses to an OutputStream.
 * It supports writing integers, simple strings, bulk strings, null bulk strings, maps, and arrays of strings.
 */
public class OutputHandler implements Closeable {
    private OutputStream out;

    /**
     * Constructs an OutputHandler with the specified OutputStream.
     *
     * @param out the OutputStream to be written to
     */
    public OutputHandler(OutputStream out) {
        this.out = out;
    }

    /**
     * Writes an integer response to the OutputStream.
     *
     * @param i the integer to be written
     * @throws IOException if an I/O error occurs
     */
    public void writeInteger(int i) throws IOException {
        String integer = ":%d\r\n".formatted(i);
        out.write(integer.getBytes());
    }

    /**
     * Writes a simple string response to the OutputStream.
     *
     * @param string the string to be written
     * @throws IOException if an I/O error occurs
     */
    public void writeSimpleString(String string) throws IOException {
        String simpleString = "+%s\r\n".formatted(string);
        out.write(simpleString.getBytes());
    }

    /**
     * Writes a bulk string response to the OutputStream.
     *
     * @param string the string to be written
     * @throws IOException if an I/O error occurs
     */
    public void writeBulkString(String string) throws IOException {
        int length = string.length();
        String bulkString = "$" + length + "\r\n" + string + "\r\n";
        out.write(bulkString.getBytes());
    }

    /**
     * Writes a bulk string response in byte array format to the OutputStream.
     *
     * @param b the byte array to be written
     * @throws IOException if an I/O error occurs
     */
    public void writeBulkString(byte[] b) throws IOException {
        out.write(("$" + b.length + "\r\n").getBytes());
        out.write(b);
        out.write("\r\n".getBytes());
    }

    /**
     * Writes a null bulk string response to the OutputStream.
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeNullBulkString() throws IOException {
        String nullBulkString = "$-1\r\n";
        out.write(nullBulkString.getBytes());
    }

    /**
     * Writes a map as a bulk string response to the OutputStream.
     *
     * @param map the map to be written
     * @throws IOException if an I/O error occurs
     */
    public void writeBulkString(Map<String, String> map) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\r\n");
            }
            sb.append("%s:%s".formatted(entry.getKey(), entry.getValue()));
        }
        writeBulkString(sb.toString());
    }

    /**
     * Writes an array of strings as a bulk string array response to the OutputStream.
     *
     * @param strings the array of strings to be written
     * @throws IOException if an I/O error occurs
     */
    public void writeBulkStringArray(String... strings) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(strings.length).append("\r\n");
        for (String string : strings) {
            sb.append("$")
                    .append(string.length())
                    .append("\r\n")
                    .append(string)
                    .append("\r\n");
        }
        out.write(sb.toString().getBytes());
    }

    /**
     * Constructs a bulk string array in byte array format.
     *
     * @param strings the array of strings to be converted
     * @return a byte array representing the bulk string array
     */
    public byte[] getBulkStringArray(String... strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(strings.length).append("\r\n");
        for (String string : strings) {
            sb.append("$")
                    .append(string.length())
                    .append("\r\n")
                    .append(string)
                    .append("\r\n");
        }
        return sb.toString().getBytes();
    }

    /**
     * Closes the OutputStream and releases any system resources associated with it.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        out.close();
    }
}
