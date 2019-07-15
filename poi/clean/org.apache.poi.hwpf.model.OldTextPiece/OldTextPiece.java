import org.apache.poi.hwpf.model.PieceDescriptor;
import org.apache.poi.hwpf.model.*;



import java.util.Arrays;

import org.apache.poi.util.Internal;
import org.apache.poi.util.NotImplemented;

/**
 * Lightweight representation of a text piece.
 * Works in the character domain, not the byte domain, so you
 * need to have turned byte references into character
 * references before getting here.
 */
@Internal
public class OldTextPiece extends TextPiece {

    private final byte[] rawBytes;

    /**
     * @param start Beginning offset in main document stream, in characters.
     * @param end   Ending offset in main document stream, in characters.
     * @param text  The raw bytes of our text
     */
    public OldTextPiece(int start, int end, byte[] text, PieceDescriptor pd) {
        super(start, end, text, pd);
        this.rawBytes = text;
    }

    /**
     * @return nothing, ever. Always throws an UnsupportedOperationException
     * @throws UnsupportedOperationException
     */
    @NotImplemented
    @Override
    public boolean isUnicode() {
        throw new UnsupportedOperationException();
    }


    @Override
    public StringBuilder getStringBuilder() {
        return (StringBuilder) _buf;
    }

    @Override
    public byte[] getRawBytes() {
        byte[] buf = new byte[rawBytes.length];
        System.arraycopy(rawBytes, 0, buf, 0, rawBytes.length);
        return buf;
    }

    /**
     * Returns part of the string.
     * Works only in characters, not in bytes!
     *
     * @param start Local start position, in characters
     * @param end   Local end position, in characters
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @NotImplemented
    public String substring(int start, int end) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not implemented for OldTextPiece.
     * Always throws UnsupportedOperationException
     */
    @Deprecated
    @NotImplemented
    public void adjustForDelete(int start, int length) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the length, in bytes
     */
    public int bytesLength() {
        return rawBytes.length;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42; // any arbitrary constant will do
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OldTextPiece &&
            Arrays.equals(rawBytes, ((OldTextPiece)other).rawBytes);
    }

    public String toString() {
        return "OldTextPiece from " + getStart() + " to " + getEnd() + " ("
                + getPieceDescriptor() + ")";
    }

}
