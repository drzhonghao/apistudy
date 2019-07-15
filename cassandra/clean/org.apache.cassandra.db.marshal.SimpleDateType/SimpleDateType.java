import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.SimpleDateSerializer;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

public class SimpleDateType extends AbstractType<Integer>
{
    public static final SimpleDateType instance = new SimpleDateType();

    SimpleDateType() {super(ComparisonType.BYTE_ORDER);} // singleton

    public ByteBuffer fromString(String source) throws MarshalException
    {
        return ByteBufferUtil.bytes(SimpleDateSerializer.dateStringToDays(source));
    }

    public ByteBuffer fromTimeInMillis(long millis) throws MarshalException
    {
        return ByteBufferUtil.bytes(SimpleDateSerializer.timeInMillisToDay(millis));
    }

    public long toTimeInMillis(ByteBuffer buffer) throws MarshalException
    {
        return SimpleDateSerializer.dayToTimeInMillis(ByteBufferUtil.toInt(buffer));
    }

    @Override
    public boolean isValueCompatibleWithInternal(AbstractType<?> otherType)
    {
        return this == otherType || otherType == Int32Type.instance;
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
                    "Expected a string representation of a date value, but got a %s: %s",
                    parsed.getClass().getSimpleName(), parsed));
        }
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return '"' + SimpleDateSerializer.instance.toString(SimpleDateSerializer.instance.deserialize(buffer)) + '"';
    }

    @Override
    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.DATE;
    }

    public TypeSerializer<Integer> getSerializer()
    {
        return SimpleDateSerializer.instance;
    }
}
