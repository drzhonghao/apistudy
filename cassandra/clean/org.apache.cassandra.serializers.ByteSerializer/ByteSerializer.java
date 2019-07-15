import org.apache.cassandra.serializers.*;


import org.apache.cassandra.utils.ByteBufferUtil;

import java.nio.ByteBuffer;

public class ByteSerializer implements TypeSerializer<Byte>
{
    public static final ByteSerializer instance = new ByteSerializer();

    public Byte deserialize(ByteBuffer bytes)
    {
        return bytes == null || bytes.remaining() == 0 ? null : bytes.get(bytes.position());
    }

    public ByteBuffer serialize(Byte value)
    {
        return value == null ? ByteBufferUtil.EMPTY_BYTE_BUFFER : ByteBuffer.allocate(1).put(0, value);
    }

    public void validate(ByteBuffer bytes) throws MarshalException
    {
        if (bytes.remaining() != 1)
            throw new MarshalException(String.format("Expected 1 byte for a tinyint (%d)", bytes.remaining()));
    }

    public String toString(Byte value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    public Class<Byte> getType()
    {
        return Byte.class;
    }
}
