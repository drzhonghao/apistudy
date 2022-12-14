import org.apache.poi.hdgf.pointers.Pointer;
import org.apache.poi.hdgf.pointers.*;


import org.apache.poi.util.LittleEndian;

/**
 * A Pointer from v5
 */
public final class PointerV5 extends Pointer {
    // TODO Are these getters correct?
    public boolean destinationHasStrings() {
        return isFormatBetween(0x40, 0x50);
    }
    
    public boolean destinationHasPointers() {
        switch (getType()) {
            case 20:
                return true;
            case 22:
                return false;
            default:
                return isFormatBetween(0x1d, 0x1f) || isFormatBetween(0x50, 0x60);
        }
    }
 
    public boolean destinationHasChunks() {
        switch (getType()) {
            case 21:
                return true;
            case 24:
                return true;
            default:
                return isFormatBetween(0xd0, 0xdf);
        }
    }

    public boolean destinationCompressed() {
        // Apparently, it's the second least significant bit
        return (getFormat() & 2) > 0;
    }

    /**
     * With v6 pointers, the on-disk size is 16 bytes
     */
    public int getSizeInBytes() { return 16; }

    /**
     * Depends on the type only, not stored
     */
    public int getNumPointersOffset(byte[] data) {
        switch (getType()) {
            case 0x1d:
            case 0x4e:
                return 30;
            case 0x1e:
                return 54;
            case 0x14:
                return 130;
        }
        return 10;
    }
    /**
     * 16 bit int at the given offset
     */
    public int getNumPointers(int offset, byte[] data) {
        // V5 stores it as a 16 bit number at the offset
        return LittleEndian.getShort(data, offset);
    }
    /**
     * Just the 2 bytes of the number of pointers
     */
    public int getPostNumPointersSkip() {
        return 2;
    }
}
