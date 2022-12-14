import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.*;


import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * dump data in hexadecimal format
 */
@Internal
public class HexDump {
    public static final String EOL = System.getProperty("line.separator");
    public static final Charset UTF8 = StandardCharsets.UTF_8;

    private HexDump() {
        // all static methods, so no need for a public constructor
    }

    /**
     * dump an array of bytes to an OutputStream
     *
     * @param data the byte array to be dumped
     * @param offset its offset, whatever that might mean
     * @param stream the OutputStream to which the data is to be
     *               written
     * @param index initial index into the byte array
     * @param length number of characters to output
     *
     * @exception IOException is thrown if anything goes wrong writing
     *            the data to stream
     * @exception ArrayIndexOutOfBoundsException if the index is
     *            outside the data array's bounds
     * @exception IllegalArgumentException if the output stream is
     *            null
     */
    public static void dump(final byte [] data, final long offset,
        final OutputStream stream, final int index, final int length)
    throws IOException, ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (stream == null) {
            throw new IllegalArgumentException("cannot write to nullstream");
        }

        OutputStreamWriter osw = new OutputStreamWriter(stream, UTF8);
        osw.write(dump(data, offset, index, length));
        osw.flush();
    }

    /**
     * dump an array of bytes to an OutputStream
     *
     * @param data the byte array to be dumped
     * @param offset its offset, whatever that might mean
     * @param stream the OutputStream to which the data is to be
     *               written
     * @param index initial index into the byte array
     *
     * @exception IOException is thrown if anything goes wrong writing
     *            the data to stream
     * @exception ArrayIndexOutOfBoundsException if the index is
     *            outside the data array's bounds
     * @exception IllegalArgumentException if the output stream is
     *            null
     */

    public synchronized static void dump(final byte [] data, final long offset,
            final OutputStream stream, final int index)
    throws IOException, ArrayIndexOutOfBoundsException, IllegalArgumentException {
        dump(data, offset, stream, index, Integer.MAX_VALUE);
    }

    /**
     * dump an array of bytes to a String
     *
     * @param data the byte array to be dumped
     * @param offset its offset, whatever that might mean
     * @param index initial index into the byte array
     *
     * @exception ArrayIndexOutOfBoundsException if the index is
     *            outside the data array's bounds
     * @return output string
     */

    public static String dump(final byte [] data, final long offset, final int index) {
        return dump(data, offset, index, Integer.MAX_VALUE);
    }    
    
    /**
     * dump an array of bytes to a String
     *
     * @param data the byte array to be dumped
     * @param offset its offset, whatever that might mean
     * @param index initial index into the byte array
     * @param length number of characters to output
     *
     * @exception ArrayIndexOutOfBoundsException if the index is
     *            outside the data array's bounds
     * @return output string
     */

    public static String dump(final byte [] data, final long offset, final int index, final int length) {
        if (data == null || data.length == 0) {
            return "No Data"+EOL;
        }

        int data_length = (length == Integer.MAX_VALUE || length < 0 || index+length < 0)
            ? data.length
            : Math.min(data.length,index+length);
        
        
        if ((index < 0) || (index >= data.length)) {
            String err = "illegal index: "+index+" into array of length "+data.length;
            throw new ArrayIndexOutOfBoundsException(err);
        }
        
        long  display_offset = offset + index;
        StringBuilder buffer = new StringBuilder(74);
        
        for (int j = index; j < data_length; j += 16) {
            int chars_read = data_length - j;

            if (chars_read > 16) {
                chars_read = 16;
            }
            
            writeHex(buffer, display_offset, 8, "");
            for (int k = 0; k < 16; k++) {
                if (k < chars_read) {
                    writeHex(buffer, data[ k + j ], 2, " ");
                } else {
                    buffer.append("   ");
                }
            }
            buffer.append(' ');
            for (int k = 0; k < chars_read; k++) {
                buffer.append(toAscii(data[ k + j ]));
            }
            buffer.append(EOL);
            display_offset += chars_read;
        }
        return buffer.toString();
    }

    public static char toAscii(int dataB) {
        char charB = (char)(dataB & 0xFF);
        if (Character.isISOControl(charB)) {
            return '.';
        }
        
        switch (charB) {
            // printable, but not compilable with current compiler encoding
            case 0xFF:
            case 0xDD:
                charB = '.';
                break;
            default:
                break;
        }
        return charB;
    }
    
    /**
     * Converts the parameter to a hex value.
     *
     * @param value     The value to convert
     * @return          A String representing the array of bytes
     */
    public static String toHex(final byte[] value)
    {
        StringBuilder retVal = new StringBuilder();
        retVal.append('[');
        if (value != null && value.length > 0)
        {
            for(int x = 0; x < value.length; x++)
            {
                if (x>0) {
                    retVal.append(", ");
                }
                retVal.append(toHex(value[x]));
            }
        }
        retVal.append(']');
        return retVal.toString();
    }

    /**
     * Converts the parameter to a hex value.
     *
     * @param value     The value to convert
     * @return          A String representing the array of shorts
     */
    public static String toHex(final short[] value)
    {
        StringBuilder retVal = new StringBuilder();
        retVal.append('[');
        for(int x = 0; x < value.length; x++)
        {
            if (x>0) {
                retVal.append(", ");
            }
            retVal.append(toHex(value[x]));
        }
        retVal.append(']');
        return retVal.toString();
    }

    /**
     * <p>Converts the parameter to a hex value breaking the results into
     * lines.</p>
     *
     * @param value        The value to convert
     * @param bytesPerLine The maximum number of bytes per line. The next byte
     *                     will be written to a new line
     * @return             A String representing the array of bytes
     */
    public static String toHex(final byte[] value, final int bytesPerLine) {
        if (value.length == 0) {
            return ": 0";
        }
        final int digits = (int) Math.round(Math.log(value.length) / Math.log(10) + 0.5);
        StringBuilder retVal = new StringBuilder();
        writeHex(retVal, 0, digits, "");
        retVal.append(": ");
        for(int x=0, i=-1; x < value.length; x++) {
            if (++i == bytesPerLine) {
                retVal.append('\n');
                writeHex(retVal, x, digits, "");
                retVal.append(": ");
                i = 0;
            } else if (x>0) {
                retVal.append(", ");
            }
            retVal.append(toHex(value[x]));
        }
        return retVal.toString();
    }

    /**
     * Converts the parameter to a hex value.
     *
     * @param value     The value to convert
     * @return          The result right padded with 0
     */
    public static String toHex(short value) {
        StringBuilder sb = new StringBuilder(4);
        writeHex(sb, value & 0xFFFF, 4, "");
        return sb.toString();
    }

    /**
     * Converts the parameter to a hex value.
     *
     * @param value     The value to convert
     * @return          The result right padded with 0
     */
    public static String toHex(byte value) {
        StringBuilder sb = new StringBuilder(2);
        writeHex(sb, value & 0xFF, 2, "");
        return sb.toString();
    }

    /**
     * Converts the parameter to a hex value.
     *
     * @param value     The value to convert
     * @return          The result right padded with 0
     */
    public static String toHex(int value) {
        StringBuilder sb = new StringBuilder(8);
        writeHex(sb, value & 0xFFFFFFFFL, 8, "");
        return sb.toString();
    }

    /**
     * Converts the parameter to a hex value.
     *
     * @param value     The value to convert
     * @return          The result right padded with 0
     */
    public static String toHex(long value) {
        StringBuilder sb = new StringBuilder(16);
        writeHex(sb, value, 16, "");
        return sb.toString();
    }

    /**
     * Converts the string to a string of hex values.
     *
     * @param value     The value to convert
     * @return          The resulted hex string
     */
    public static String toHex(String value) {
        return (value == null || value.length() == 0)
            ? "[]"
            : toHex(value.getBytes(LocaleUtil.CHARSET_1252));
    }
    
    /**
     * Dumps <code>bytesToDump</code> bytes to an output stream.
     *
     * @param in          The stream to read from
     * @param out         The output stream
     * @param start       The index to use as the starting position for the left hand side label
     * @param bytesToDump The number of bytes to output.  Use -1 to read until the end of file.
     */
    public static void dump( InputStream in, PrintStream out, int start, int bytesToDump ) throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        if (bytesToDump == -1)
        {
            int c = in.read();
            while (c != -1)
            {
                buf.write(c);
                c = in.read();
            }
        }
        else
        {
            int bytesRemaining = bytesToDump;
            while (bytesRemaining-- > 0)
            {
                int c = in.read();
                if (c == -1) {
                    break;
                }
                buf.write(c);
            }
        }

        byte[] data = buf.toByteArray();
        dump(data, 0, out, start, data.length);
    }

    /**
     * @return string of 16 (zero padded) uppercase hex chars and prefixed with '0x'
     */
    public static String longToHex(long value) {
        StringBuilder sb = new StringBuilder(18);
        writeHex(sb, value, 16, "0x");
        return sb.toString();
    }
    
    /**
     * @return string of 8 (zero padded) uppercase hex chars and prefixed with '0x'
     */
    public static String intToHex(int value) {
        StringBuilder sb = new StringBuilder(10);
        writeHex(sb, value & 0xFFFFFFFFL, 8, "0x");
        return sb.toString();
    }
    
    /**
     * @return string of 4 (zero padded) uppercase hex chars and prefixed with '0x'
     */
    public static String shortToHex(int value) {
        StringBuilder sb = new StringBuilder(6);
        writeHex(sb, value & 0xFFFFL, 4, "0x");
        return sb.toString();
    }
    
    /**
     * @return string of 2 (zero padded) uppercase hex chars and prefixed with '0x'
     */
    public static String byteToHex(int value) {
        StringBuilder sb = new StringBuilder(4);
        writeHex(sb, value & 0xFFL, 2, "0x");
        return sb.toString();
    }
    
    /**
     * @see Integer#toHexString(int)
     * @see Long#toHexString(long)
     */
    private static void writeHex(StringBuilder sb, long value, int nDigits, String prefix) {
        sb.append(prefix);
        char[] buf = new char[nDigits];
        long acc = value;
        for(int i=nDigits-1; i>=0; i--) {
            int digit = (int)(acc & 0x0F);
            buf[i] = (char) (digit < 10 ? ('0' + digit) : ('A' + digit - 10));
            acc >>>= 4;
        }
        sb.append(buf);
    }    
    
    
    public static void main(String[] args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        byte[] b = IOUtils.toByteArray(in);
        in.close();
        System.out.println(HexDump.dump(b, 0, 0));
    }
}
