import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.FloatSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;


public class FloatType extends AbstractType<Float>
{
    public static final FloatType instance = new FloatType();

    FloatType() {super(ComparisonType.CUSTOM);} // singleton

    public boolean isEmptyValueMeaningless()
    {
        return true;
    }

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        if (!o1.hasRemaining() || !o2.hasRemaining())
            return o1.hasRemaining() ? 1 : o2.hasRemaining() ? -1 : 0;

        return compose(o1).compareTo(compose(o2));
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
      // Return an empty ByteBuffer for an empty string.
      if (source.isEmpty())
          return ByteBufferUtil.EMPTY_BYTE_BUFFER;

      try
      {
          float f = Float.parseFloat(source);
          return ByteBufferUtil.bytes(f);
      }
      catch (NumberFormatException e1)
      {
          throw new MarshalException(String.format("Unable to make float from '%s'", source), e1);
      }
    }

    @Override
    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        try
        {
            if (parsed instanceof String)
                return new Constants.Value(fromString((String) parsed));
            else
                return new Constants.Value(getSerializer().serialize(((Number) parsed).floatValue()));
        }
        catch (ClassCastException exc)
        {
            throw new MarshalException(String.format(
                    "Expected a float value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
        }
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return getSerializer().deserialize(buffer).toString();
    }

    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.FLOAT;
    }

    public TypeSerializer<Float> getSerializer()
    {
        return FloatSerializer.instance;
    }

    @Override
    protected int valueLengthIfFixed()
    {
        return 4;
    }
}
