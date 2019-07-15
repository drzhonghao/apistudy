import org.apache.cassandra.db.*;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.cassandra.utils.vint.VIntCoding;

public final class TypeSizes
{

    private TypeSizes(){}

    private static final int BOOL_SIZE = 1;
    private static final int BYTE_SIZE = 1;
    private static final int SHORT_SIZE = 2;
    private static final int INT_SIZE = 4;
    private static final int LONG_SIZE = 8;
    private static final int UUID_SIZE = 16;

    /** assumes UTF8 */
    public static int sizeof(String value)
    {
        int length = encodedUTF8Length(value);
        assert length <= Short.MAX_VALUE;
        return sizeof((short) length) + length;
    }

    public static int encodedUTF8Length(String st)
    {
        int strlen = st.length();
        int utflen = 0;
        for (int i = 0; i < strlen; i++)
        {
            int c = st.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F))
                utflen++;
            else if (c > 0x07FF)
                utflen += 3;
            else
                utflen += 2;
        }
        return utflen;
    }

    public static int sizeofWithShortLength(ByteBuffer value)
    {
        return sizeof((short) value.remaining()) + value.remaining();
    }

    public static int sizeofWithLength(ByteBuffer value)
    {
        return sizeof(value.remaining()) + value.remaining();
    }

    public static int sizeofWithVIntLength(ByteBuffer value)
    {
        return sizeofUnsignedVInt(value.remaining()) + value.remaining();
    }

    public static int sizeof(boolean value)
    {
        return BOOL_SIZE;
    }

    public static int sizeof(byte value)
    {
        return BYTE_SIZE;
    }

    public static int sizeof(short value)
    {
        return SHORT_SIZE;
    }

    public static int sizeof(int value)
    {
        return INT_SIZE;
    }

    public static int sizeof(long value)
    {
        return LONG_SIZE;
    }

    public static int sizeof(UUID value)
    {
        return UUID_SIZE;
    }

    public static int sizeofVInt(long value)
    {
        return VIntCoding.computeVIntSize(value);
    }

    public static int sizeofUnsignedVInt(long value)
    {
        return VIntCoding.computeUnsignedVIntSize(value);
    }
}
