import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Duration;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.DurationSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Represents a duration. The duration is stored as  months, days, and nanoseconds. This is done
 * <p>Internally he duration is stored as months (unsigned integer), days (unsigned integer), and nanoseconds.</p>
 */
public class DurationType extends AbstractType<Duration>
{
    public static final DurationType instance = new DurationType();

    DurationType()
    {
        super(ComparisonType.BYTE_ORDER);
    } // singleton

    public ByteBuffer fromString(String source) throws MarshalException
    {
        // Return an empty ByteBuffer for an empty string.
        if (source.isEmpty())
            return ByteBufferUtil.EMPTY_BYTE_BUFFER;

        return decompose(Duration.from(source));
    }

    @Override
    public boolean isValueCompatibleWithInternal(AbstractType<?> otherType)
    {
        return this == otherType;
    }

    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        try
        {
            return new Constants.Value(fromString((String) parsed));
        }
        catch (ClassCastException exc)
        {
            throw new MarshalException(String.format("Expected a string representation of a duration, but got a %s: %s",
                                                     parsed.getClass().getSimpleName(), parsed));
        }
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return getSerializer().deserialize(buffer).toString();
    }

    @Override
    public TypeSerializer<Duration> getSerializer()
    {
        return DurationSerializer.instance;
    }

    @Override
    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.DURATION;
    }

    @Override
    public boolean referencesDuration()
    {
        return true;
    }
}
