import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.serializers.EmptySerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * A type that only accept empty data.
 * It is only useful as a value validation type, not as a comparator since column names can't be empty.
 */
public class EmptyType extends AbstractType<Void>
{
    public static final EmptyType instance = new EmptyType();

    private EmptyType() {super(ComparisonType.CUSTOM);} // singleton

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        return 0;
    }

    public String getString(ByteBuffer bytes)
    {
        return "";
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
        if (!source.isEmpty())
            throw new MarshalException(String.format("'%s' is not empty", source));

        return ByteBufferUtil.EMPTY_BYTE_BUFFER;
    }

    @Override
    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        if (!(parsed instanceof String))
            throw new MarshalException(String.format("Expected an empty string, but got: %s", parsed));
        if (!((String) parsed).isEmpty())
            throw new MarshalException(String.format("'%s' is not empty", parsed));

        return new Constants.Value(ByteBufferUtil.EMPTY_BYTE_BUFFER);
    }

    @Override
    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.EMPTY;
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return "\"\"";
    }

    public TypeSerializer<Void> getSerializer()
    {
        return EmptySerializer.instance;
    }

    @Override
    protected int valueLengthIfFixed()
    {
        return 0;
    }
}
