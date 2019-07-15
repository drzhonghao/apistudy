import org.apache.cassandra.serializers.*;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.UUIDGen;

public class UUIDSerializer implements TypeSerializer<UUID>
{
    public static final UUIDSerializer instance = new UUIDSerializer();

    public UUID deserialize(ByteBuffer bytes)
    {
        return bytes.remaining() == 0 ? null : UUIDGen.getUUID(bytes);
    }

    public ByteBuffer serialize(UUID value)
    {
        return value == null ? ByteBufferUtil.EMPTY_BYTE_BUFFER : UUIDGen.toByteBuffer(value);
    }

    public void validate(ByteBuffer bytes) throws MarshalException
    {
        if (bytes.remaining() != 16 && bytes.remaining() != 0)
            throw new MarshalException(String.format("UUID should be 16 or 0 bytes (%d)", bytes.remaining()));
        // not sure what the version should be for this.
    }

    public String toString(UUID value)
    {
        return value == null ? "" : value.toString();
    }

    public Class<UUID> getType()
    {
        return UUID.class;
    }
}
