import org.apache.poi.hdgf.chunks.ChunkHeader;
import org.apache.poi.hdgf.chunks.*;


import java.nio.charset.Charset;

/**
 * A chunk header from v6
 */
public class ChunkHeaderV6 extends ChunkHeader {
	private short unknown2;
	private short unknown3;

	public short getUnknown2() {
		return unknown2;
	}
	public short getUnknown3() {
		return unknown3;
	}

	protected static int getHeaderSize() {
		// Looks like it ought to be 19...
		return 19;
	}
	public int getSizeInBytes() {
		return getHeaderSize();
	}

	/**
	 * Does the chunk have a trailer?
	 */
	public boolean hasTrailer() {
	    switch (getType()) {
    	    case 0x2c: case 0x65: case 0x66: case 0x69:
    	    case 0x6a: case 0x6b: case 0x70: case 0x71:
    	        return true;
            default:
                return (getUnknown1() != 0);
	    }
	}

	/**
	 * Does the chunk have a separator?
	 */
	public boolean hasSeparator() {
		// V6 never has separators
		return false;
	}

	@Override
	public Charset getChunkCharset() {
		return Charset.forName("ASCII");
	}

    void setUnknown2(short unknown2) {
        this.unknown2 = unknown2;
    }

    void setUnknown3(short unknown3) {
        this.unknown3 = unknown3;
    }
}
