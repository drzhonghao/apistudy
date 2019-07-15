import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.TimeSerializer;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.transport.ProtocolVersion;

/**
 * Nanosecond resolution time values
 */
public class TimeType extends AbstractType<Long>
{
    public static final TimeType instance = new TimeType();
    private TimeType() {super(ComparisonType.BYTE_ORDER);} // singleton

    public ByteBuffer fromString(String source) throws MarshalException
    {
        return decompose(TimeSerializer.timeStringToLong(source));
    }

    @Override
    public boolean isValueCompatibleWithInternal(AbstractType<?> otherType)
    {
        return this == otherType || otherType == LongType.instance;
    }

    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        try
        {
            return new Constants.Value(fromString((String) parsed));
        }
        catch (ClassCastException exc)
        {
            throw new MarshalException(String.format(
                    "Expected a string representation of a time value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
        }
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return '"' + TimeSerializer.instance.toString(TimeSerializer.instance.deserialize(buffer)) + '"';
    }

    @Override
    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.TIME;
    }

    public TypeSerializer<Long> getSerializer()
    {
        return TimeSerializer.instance;
    }
}
