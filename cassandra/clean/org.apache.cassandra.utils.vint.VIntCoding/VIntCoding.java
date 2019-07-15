import org.apache.cassandra.utils.vint.*;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import io.netty.util.concurrent.FastThreadLocal;
import net.nicoulaj.compilecommand.annotations.Inline;

/**
 * Borrows idea from
 * https://developers.google.com/protocol-buffers/docs/encoding#varints
 */
public class VIntCoding
{

    public static long readUnsignedVInt(DataInput input) throws IOException
    {
        int firstByte = input.readByte();

        //Bail out early if this is one byte, necessary or it fails later
        if (firstByte >= 0)
            return firstByte;

        int size = numberOfExtraBytesToRead(firstByte);
        long retval = firstByte & firstByteValueMask(size);
        for (int ii = 0; ii < size; ii++)
        {
            byte b = input.readByte();
            retval <<= 8;
            retval |= b & 0xff;
        }

        return retval;
    }

    public static long readVInt(DataInput input) throws IOException
    {
        return decodeZigZag64(readUnsignedVInt(input));
    }

    // & this with the first byte to give the value part for a given extraBytesToRead encoded in the byte
    public static int firstByteValueMask(int extraBytesToRead)
    {
        // by including the known 0bit in the mask, we can use this for encodeExtraBytesToRead
        return 0xff >> extraBytesToRead;
    }

    public static int encodeExtraBytesToRead(int extraBytesToRead)
    {
        // because we have an extra bit in the value mask, we just need to invert it
        return ~firstByteValueMask(extraBytesToRead);
    }

    public static int numberOfExtraBytesToRead(int firstByte)
    {
        // we count number of set upper bits; so if we simply invert all of the bits, we're golden
        // this is aided by the fact that we only work with negative numbers, so when upcast to an int all
        // of the new upper bits are also set, so by inverting we set all of them to zero
        return Integer.numberOfLeadingZeros(~firstByte) - 24;
    }

    protected static final FastThreadLocal<byte[]> encodingBuffer = new FastThreadLocal<byte[]>()
    {
        @Override
        public byte[] initialValue()
        {
            return new byte[9];
        }
    };

    public static void writeUnsignedVInt(long value, DataOutput output) throws IOException
    {
        int size = VIntCoding.computeUnsignedVIntSize(value);
        if (size == 1)
        {
            output.write((int)value);
            return;
        }

        output.write(VIntCoding.encodeVInt(value, size), 0, size);
    }

    @Inline
    public static byte[] encodeVInt(long value, int size)
    {
        byte encodingSpace[] = encodingBuffer.get();
        int extraBytes = size - 1;

        for (int i = extraBytes ; i >= 0; --i)
        {
            encodingSpace[i] = (byte) value;
            value >>= 8;
        }
        encodingSpace[0] |= VIntCoding.encodeExtraBytesToRead(extraBytes);
        return encodingSpace;
    }

    public static void writeVInt(long value, DataOutput output) throws IOException
    {
        writeUnsignedVInt(encodeZigZag64(value), output);
    }

    /**
     * Decode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 64-bit integer, stored in a signed int because
     *          Java has no explicit unsigned support.
     * @return A signed 64-bit integer.
     */
    public static long decodeZigZag64(final long n)
    {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * Encode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 64-bit integer.
     * @return An unsigned 64-bit integer, stored in a signed int because
     *         Java has no explicit unsigned support.
     */
    public static long encodeZigZag64(final long n)
    {
        // Note:  the right-shift must be arithmetic
        return (n << 1) ^ (n >> 63);
    }

    /** Compute the number of bytes that would be needed to encode a varint. */
    public static int computeVIntSize(final long param)
    {
        return computeUnsignedVIntSize(encodeZigZag64(param));
    }

    /** Compute the number of bytes that would be needed to encode an unsigned varint. */
    public static int computeUnsignedVIntSize(final long value)
    {
        int magnitude = Long.numberOfLeadingZeros(value | 1); // | with 1 to ensure magntiude <= 63, so (63 - 1) / 7 <= 8
        return 9 - ((magnitude - 1) / 7);
    }
}
