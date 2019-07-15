import org.apache.cassandra.serializers.*;


import org.apache.cassandra.utils.ByteBufferUtil;

import java.nio.ByteBuffer;

public class BytesSerializer implements TypeSerializer<ByteBuffer>
{
    public static final BytesSerializer instance = new BytesSerializer();

    public ByteBuffer serialize(ByteBuffer bytes)
    {
        // We make a copy in case the user modifies the input
        return bytes.duplicate();
    }

    public ByteBuffer deserialize(ByteBuffer value)
    {
        // This is from the DB, so it is not shared with someone else
        return value;
    }

    public void validate(ByteBuffer bytes) throws MarshalException
    {
        // all bytes are legal.
    }

    public String toString(ByteBuffer value)
    {
        return ByteBufferUtil.bytesToHex(value);
    }

    public Class<ByteBuffer> getType()
    {
        return ByteBuffer.class;
    }

    @Override
    public String toCQLLiteral(ByteBuffer buffer)
    {
        return buffer == null
             ? "null"
             : "0x" + toString(deserialize(buffer));
    }
}
