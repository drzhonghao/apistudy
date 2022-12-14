import org.apache.cassandra.serializers.*;


import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

public abstract class CollectionSerializer<T> implements TypeSerializer<T>
{
    protected abstract List<ByteBuffer> serializeValues(T value);
    protected abstract int getElementCount(T value);

    public abstract T deserializeForNativeProtocol(ByteBuffer buffer, ProtocolVersion version);
    public abstract void validateForNativeProtocol(ByteBuffer buffer, ProtocolVersion version);

    public ByteBuffer serialize(T value)
    {
        List<ByteBuffer> values = serializeValues(value);
        // See deserialize() for why using the protocol v3 variant is the right thing to do.
        return pack(values, getElementCount(value), ProtocolVersion.V3);
    }

    public T deserialize(ByteBuffer bytes)
    {
        // The only cases we serialize/deserialize collections internally (i.e. not for the protocol sake),
        // is:
        //  1) when collections are frozen
        //  2) for internal calls.
        // In both case, using the protocol 3 version variant is the right thing to do.
        return deserializeForNativeProtocol(bytes, ProtocolVersion.V3);
    }

    public void validate(ByteBuffer bytes) throws MarshalException
    {
        // Same thing as above
        validateForNativeProtocol(bytes, ProtocolVersion.V3);
    }

    public static ByteBuffer pack(Collection<ByteBuffer> buffers, int elements, ProtocolVersion version)
    {
        int size = 0;
        for (ByteBuffer bb : buffers)
            size += sizeOfValue(bb, version);

        ByteBuffer result = ByteBuffer.allocate(sizeOfCollectionSize(elements, version) + size);
        writeCollectionSize(result, elements, version);
        for (ByteBuffer bb : buffers)
            writeValue(result, bb, version);
        return (ByteBuffer)result.flip();
    }

    protected static void writeCollectionSize(ByteBuffer output, int elements, ProtocolVersion version)
    {
            output.putInt(elements);
    }

    public static int readCollectionSize(ByteBuffer input, ProtocolVersion version)
    {
        return input.getInt();
    }

    protected static int sizeOfCollectionSize(int elements, ProtocolVersion version)
    {
        return 4;
    }

    public static void writeValue(ByteBuffer output, ByteBuffer value, ProtocolVersion version)
    {
        if (value == null)
        {
            output.putInt(-1);
            return;
        }

        output.putInt(value.remaining());
        output.put(value.duplicate());
    }

    public static ByteBuffer readValue(ByteBuffer input, ProtocolVersion version)
    {
        int size = input.getInt();
        if (size < 0)
            return null;

        return ByteBufferUtil.readBytes(input, size);
    }

    public static int sizeOfValue(ByteBuffer value, ProtocolVersion version)
    {
        return value == null ? 4 : 4 + value.remaining();
    }
}
