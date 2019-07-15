import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.BooleanSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.transport.ProtocolVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BooleanType extends AbstractType<Boolean>
{
    private static final Logger logger = LoggerFactory.getLogger(BooleanType.class);

    public static final BooleanType instance = new BooleanType();

    BooleanType() {super(ComparisonType.CUSTOM);} // singleton

    public boolean isEmptyValueMeaningless()
    {
        return true;
    }

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        if (!o1.hasRemaining() || !o2.hasRemaining())
            return o1.hasRemaining() ? 1 : o2.hasRemaining() ? -1 : 0;

        // False is 0, True is anything else, makes False sort before True.
        byte b1 = o1.get(o1.position());
        byte b2 = o2.get(o2.position());
        if (b1 == 0)
            return b2 == 0 ? 0 : -1;
        return b2 == 0 ? 1 : 0;
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {

        if (source.isEmpty()|| source.equalsIgnoreCase(Boolean.FALSE.toString()))
            return decompose(false);

        if (source.equalsIgnoreCase(Boolean.TRUE.toString()))
            return decompose(true);

        throw new MarshalException(String.format("Unable to make boolean from '%s'", source));
    }

    @Override
    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        if (parsed instanceof String)
            return new Constants.Value(fromString((String) parsed));
        else if (!(parsed instanceof Boolean))
            throw new MarshalException(String.format(
                    "Expected a boolean value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));

        return new Constants.Value(getSerializer().serialize((Boolean) parsed));
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return getSerializer().deserialize(buffer).toString();
    }

    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.BOOLEAN;
    }

    public TypeSerializer<Boolean> getSerializer()
    {
        return BooleanSerializer.instance;
    }

    @Override
    protected int valueLengthIfFixed()
    {
        return 1;
    }
}
