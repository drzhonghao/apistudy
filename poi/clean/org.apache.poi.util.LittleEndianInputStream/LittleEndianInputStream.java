import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.*;


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an {@link InputStream} providing {@link LittleEndianInput}<p>
 *
 * This class does not buffer any input, so the stream read position maintained
 * by this class is consistent with that of the inner stream.
 */
public class LittleEndianInputStream extends FilterInputStream implements LittleEndianInput {

	private static final int EOF = -1;

	public LittleEndianInputStream(InputStream is) {
		super(is);
	}
	
	@Override
	@SuppressForbidden("just delegating")
	public int available() {
		try {
			return super.available();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public byte readByte() {
		return (byte)readUByte();
	}
	
	@Override
	public int readUByte() {
		byte buf[] = new byte[1];
		try {
			checkEOF(read(buf), 1);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return LittleEndian.getUByte(buf);
	}
	
	@Override
	public double readDouble() {
		return Double.longBitsToDouble(readLong());
	}
	
	@Override
	public int readInt() {
		byte buf[] = new byte[LittleEndianConsts.INT_SIZE];
		try {
			checkEOF(read(buf), buf.length);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return LittleEndian.getInt(buf);
	}
	
    /**
     * get an unsigned int value from an InputStream
     * 
     * @return the unsigned int (32-bit) value
     * @exception RuntimeException
     *                wraps any IOException thrown from reading the stream.
     */
    //@Override
    public long readUInt() {
       long retNum = readInt();
       return retNum & 0x00FFFFFFFFL;
    }
	
	@Override
	public long readLong() {
		byte buf[] = new byte[LittleEndianConsts.LONG_SIZE];
		try {
		    checkEOF(read(buf), LittleEndianConsts.LONG_SIZE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return LittleEndian.getLong(buf);
	}
	
	@Override
	public short readShort() {
		return (short)readUShort();
	}
	
	@Override
	public int readUShort() {
		byte buf[] = new byte[LittleEndianConsts.SHORT_SIZE];
		try {
		    checkEOF(read(buf), LittleEndianConsts.SHORT_SIZE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return LittleEndian.getUShort(buf);
	}
	
	private static void checkEOF(int actualBytes, int expectedBytes) {
		if (expectedBytes != 0 && (actualBytes == -1 || actualBytes != expectedBytes)) {
			throw new RuntimeException("Unexpected end-of-file");
		}
	}

    @Override
    public void readFully(byte[] buf) {
        readFully(buf, 0, buf.length);
    }

    @Override
    public void readFully(byte[] buf, int off, int len) {
        try {
        	checkEOF(_read(buf, off, len), len);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //Makes repeated calls to super.read() until length is read or EOF is reached
	private int _read(byte[] buffer, int offset, int length) throws IOException {
    	//lifted directly from org.apache.commons.io.IOUtils 2.4
		int remaining = length;
		while (remaining > 0) {
			int location = length - remaining;
			int count = read(buffer, offset + location, remaining);
			if (EOF == count) { // EOF
				break;
			}
			remaining -= count;
		}

		return length - remaining;
	}

    @Override
    public void readPlain(byte[] buf, int off, int len) {
        readFully(buf, off, len);
    }
}
