import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.db.marshal.DateType;
import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TimestampSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Type for date-time values.
 *
 * This is meant as a replacement for DateType, as DateType wrongly compare
 * pre-unix-epoch dates, sorting them *after* post-unix-epoch ones (due to it's
 * use of unsigned bytes comparison).
 */
public class TimestampType extends AbstractType<Date>
{
    private static final Logger logger = LoggerFactory.getLogger(TimestampType.class);

    public static final TimestampType instance = new TimestampType();

    private TimestampType() {super(ComparisonType.CUSTOM);} // singleton

    public boolean isEmptyValueMeaningless()
    {
        return true;
    }

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        return LongType.compareLongs(o1, o2);
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
      // Return an empty ByteBuffer for an empty string.
      if (source.isEmpty())
          return ByteBufferUtil.EMPTY_BYTE_BUFFER;

      return ByteBufferUtil.bytes(TimestampSerializer.dateStringToTimestamp(source));
    }

    public ByteBuffer fromTimeInMillis(long millis) throws MarshalException
    {
        return ByteBufferUtil.bytes(millis);
    }

    @Override
    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        if (parsed instanceof Long)
            return new Constants.Value(ByteBufferUtil.bytes((Long) parsed));

        try
        {
            return new Constants.Value(TimestampType.instance.fromString((String) parsed));
        }
        catch (ClassCastException exc)
        {
            throw new MarshalException(String.format(
                    "Expected a long or a datestring representation of a timestamp value, but got a %s: %s",
                    parsed.getClass().getSimpleName(), parsed));
        }
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return '"' + TimestampSerializer.getJsonDateFormatter().format(TimestampSerializer.instance.deserialize(buffer)) + '"';
    }

    @Override
    public boolean isCompatibleWith(AbstractType<?> previous)
    {
        if (super.isCompatibleWith(previous))
            return true;

        if (previous instanceof DateType)
        {
            logger.warn("Changing from DateType to TimestampType is allowed, but be wary that they sort differently for pre-unix-epoch timestamps "
                      + "(negative timestamp values) and thus this change will corrupt your data if you have such negative timestamp. So unless you "
                      + "know that you don't have *any* pre-unix-epoch timestamp you should change back to DateType");
            return true;
        }

        return false;
    }

    @Override
    public boolean isValueCompatibleWithInternal(AbstractType<?> otherType)
    {
        return this == otherType || otherType == DateType.instance || otherType == LongType.instance;
    }

    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.TIMESTAMP;
    }

    public TypeSerializer<Date> getSerializer()
    {
        return TimestampSerializer.instance;
    }

    @Override
    protected int valueLengthIfFixed()
    {
        return 8;
    }
}
