import org.apache.cassandra.db.marshal.DateType;
import org.apache.cassandra.db.marshal.TimestampType;
import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.LongSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

public class LongType extends AbstractType<Long>
{
    public static final LongType instance = new LongType();

    LongType() {super(ComparisonType.CUSTOM);} // singleton

    public boolean isEmptyValueMeaningless()
    {
        return true;
    }

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        return compareLongs(o1, o2);
    }

    public static int compareLongs(ByteBuffer o1, ByteBuffer o2)
    {
        if (!o1.hasRemaining() || !o2.hasRemaining())
            return o1.hasRemaining() ? 1 : o2.hasRemaining() ? -1 : 0;

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

        long longType;

        try
        {
            longType = Long.parseLong(source);
        }
        catch (Exception e)
        {
            throw new MarshalException(String.format("Unable to make long from '%s'", source), e);
        }

        return decompose(longType);
    }

    @Override
    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        try
        {
            if (parsed instanceof String)
                return new Constants.Value(fromString((String) parsed));

            Number parsedNumber = (Number) parsed;
            if (!(parsedNumber instanceof Integer || parsedNumber instanceof Long))
                throw new MarshalException(String.format("Expected a bigint value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));

            return new Constants.Value(getSerializer().serialize(parsedNumber.longValue()));
        }
        catch (ClassCastException exc)
        {
            throw new MarshalException(String.format(
                    "Expected a bigint value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
        }
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return getSerializer().deserialize(buffer).toString();
    }

    @Override
    public boolean isValueCompatibleWithInternal(AbstractType<?> otherType)
    {
        return this == otherType || otherType == DateType.instance || otherType == TimestampType.instance;
    }

    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.BIGINT;
    }

    public TypeSerializer<Long> getSerializer()
    {
        return LongSerializer.instance;
    }

    @Override
    protected int valueLengthIfFixed()
    {
        return 8;
    }
}
