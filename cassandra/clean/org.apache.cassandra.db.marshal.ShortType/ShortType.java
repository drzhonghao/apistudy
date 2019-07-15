import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.ShortSerializer;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

public class ShortType extends AbstractType<Short>
{
    public static final ShortType instance = new ShortType();

    ShortType()
    {
        super(ComparisonType.CUSTOM);
    } // singleton

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        int diff = o1.get(o1.position()) - o2.get(o2.position());
        if (diff != 0)
            return diff;

        return ByteBufferUtil.compareUnsigned(o1, o2);
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
        // Return an empty ByteBuffer for an empty string.
        if (source.isEmpty())
            return ByteBufferUtil.EMPTY_BYTE_BUFFER;

        short s;

        try
        {
            s = Short.parseShort(source);
        }
        catch (Exception e)
        {
            throw new MarshalException(String.format("Unable to make short from '%s'", source), e);
        }

        return decompose(s);
    }

    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        if (parsed instanceof String || parsed instanceof Number)
            return new Constants.Value(fromString(String.valueOf(parsed)));

        throw new MarshalException(String.format(
                "Expected a short value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return getSerializer().deserialize(buffer).toString();
    }

    @Override
    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.SMALLINT;
    }

    public TypeSerializer<Short> getSerializer()
    {
        return ShortSerializer.instance;
    }
}
