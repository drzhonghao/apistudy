import org.apache.poi.hdgf.pointers.Pointer;
import org.apache.poi.hdgf.pointers.*;


import org.apache.poi.util.LittleEndian;

/**
 * A Pointer from v6+
 */
public final class PointerV6 extends Pointer {
    public boolean destinationHasStrings() {
        return isFormatBetween(0x40, 0x50);
    }
    
    public boolean destinationHasPointers() {
        return (getType() == 20) || isFormatBetween(0x1d, 0x1f) || isFormatBetween(0x50, 0x60);
    }
    public boolean destinationHasChunks() {
        return isFormatBetween(0xd0, 0xdf);
    }

    public boolean destinationCompressed() {
        // Apparently, it's the second least significant bit
        return (getFormat() & 2) > 0;
    }

    /**
     * With v6 pointers, the on-disk size is 18 bytes
     */
    public int getSizeInBytes() { return 18; }

    /**
     * Stored within the data
     */
    public int getNumPointersOffset(byte[] data) {
        return getNumPointersOffsetV6(data);
    }
    public static int getNumPointersOffsetV6(byte[] data) {    
        // V6 stores it as the first value in the stream
        return (int)LittleEndian.getUInt(data, 0);
    }
    /**
     * 32 bit int at the given offset
     */
    public int getNumPointers(int offset, byte[] data) {
        return getNumPointersV6(offset, data);
    }
    public static int getNumPointersV6(int offset, byte[] data) {
        // V6 stores it a 32 bit number at the offset
        return (int)LittleEndian.getUInt(data, offset);
    }
    /**
     * 4 bytes of the number, and 4 more unknown bytes
     */
    public int getPostNumPointersSkip() {
        return getPostNumPointersSkipV6();
    }
    public static int getPostNumPointersSkipV6() {
        return 8;
    }
}
