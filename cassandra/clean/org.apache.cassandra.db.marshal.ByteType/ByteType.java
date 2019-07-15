import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.ByteSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

public class ByteType extends AbstractType<Byte>
{
    public static final ByteType instance = new ByteType();

    ByteType()
    {
        super(ComparisonType.CUSTOM);
    } // singleton

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        return o1.get(o1.position()) - o2.get(o2.position());
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
        // Return an empty ByteBuffer for an empty string.
        if (source.isEmpty())
            return ByteBufferUtil.EMPTY_BYTE_BUFFER;

        byte b;

        try
        {
            b = Byte.parseByte(source);
        }
        catch (Exception e)
        {
            throw new MarshalException(String.format("Unable to make byte from '%s'", source), e);
        }

        return decompose(b);
    }

    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        if (parsed instanceof String || parsed instanceof Number)
            return new Constants.Value(fromString(String.valueOf(parsed)));

        throw new MarshalException(String.format(
                "Expected a byte value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return getSerializer().deserialize(buffer).toString();
    }

    @Override
    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.TINYINT;
    }

    @Override
    public TypeSerializer<Byte> getSerializer()
    {
        return ByteSerializer.instance;
    }
}
