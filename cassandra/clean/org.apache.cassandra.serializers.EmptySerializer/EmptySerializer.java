import org.apache.cassandra.serializers.*;


import org.apache.cassandra.utils.ByteBufferUtil;

import java.nio.ByteBuffer;

public class EmptySerializer implements TypeSerializer<Void>
{
    public static final EmptySerializer instance = new EmptySerializer();

    public Void deserialize(ByteBuffer bytes)
    {
        return null;
    }

    public ByteBuffer serialize(Void value)
    {
        return ByteBufferUtil.EMPTY_BYTE_BUFFER;
    }

    public void validate(ByteBuffer bytes) throws MarshalException
    {
        if (bytes.remaining() > 0)
            throw new MarshalException("EmptyType only accept empty values");
    }

    public String toString(Void value)
    {
        return "";
    }

    public Class<Void> getType()
    {
        return Void.class;
    }
}
