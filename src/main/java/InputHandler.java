import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * The InputHandler class provides various methods to read and process data from an InputStream.
 * It includes functionalities to read bytes, simple strings, lines, and bulk string arrays.
 * It also keeps track of the read position within the stream.
 */
public class InputHandler implements Closeable {
    private InputStream in;
    private long position;

    /**
     * Constructs an InputHandler with the specified InputStream.
     *
     * @param in the InputStream to be read from
     */
    public InputHandler(InputStream in) {
        this.in = in;
    }

    /**
     * Returns the current read position within the InputStream.
     *
     * @return the current position
     */
    public long getPosition() {
        return position;
    }

    /**
     * Resets the read position to the beginning of the InputStream.
     */
    public void resetPosition() {
        position = 0;
    }

    /**
     * Reads a specified number of bytes from the InputStream.
     *
     * @param len the number of bytes to read
     * @return a byte array containing the bytes read from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public byte[] readNBytes(int len) throws IOException {
        position += len;
        return in.readNBytes(len);
    }

    /**
     * Reads a simple string (terminated by CRLF) from the InputStream.
     *
     * @return a string read from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public String readSimpleString() throws IOException {
        return readLine();
    }

    /**
     * Reads a line of text (terminated by CRLF) from the InputStream.
     *
     * @return a line of text read from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public String readLine() throws IOException {
        // TODO: make private
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != '\r') {
            sb.append((char) c);
        }
        c = in.read(); // consume \n
        position += sb.length() + 2; // add length of \r\n
        return sb.toString();
    }

    /**
     * Reads an array of bulk strings from the InputStream.
     * The format of the input is expected to be: *<number of elements>\r\n$<length>\r\n<string>\r\n
     *
     * @return an array of strings read from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public String[] readBulkStringArray() throws IOException {
        // Expected input format: *<number of elements>\r\n$<length>\r\n<string>\r\n
        int numberOfElements = Integer.parseInt(readLine().substring(1));
        String[] strings = new String[numberOfElements];
        for (int i = 0; i < strings.length; i++) {
            readLine();              // consume length line
            strings[i] = readLine(); // read actual string
        }
        System.out.println("command received=" + Arrays.toString(strings));
        return strings;
    }

    /**
     * Closes the InputStream and releases any system resources associated with it.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
}
