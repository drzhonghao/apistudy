import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.UUIDSerializer;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.UUIDGen;

public class LexicalUUIDType extends AbstractType<UUID>
{
    public static final LexicalUUIDType instance = new LexicalUUIDType();

    LexicalUUIDType()
    {
        super(ComparisonType.CUSTOM);
    } // singleton

    public boolean isEmptyValueMeaningless()
    {
        return true;
    }

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        if (!o1.hasRemaining() || !o2.hasRemaining())
            return o1.hasRemaining() ? 1 : o2.hasRemaining() ? -1 : 0;

        return UUIDGen.getUUID(o1).compareTo(UUIDGen.getUUID(o2));
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
        // Return an empty ByteBuffer for an empty string.
        if (source.isEmpty())
            return ByteBufferUtil.EMPTY_BYTE_BUFFER;

        try
        {
            return decompose(UUID.fromString(source));
        }
        catch (IllegalArgumentException e)
        {
            throw new MarshalException(String.format("unable to make UUID from '%s'", source), e);
        }
    }

    @Override
    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        try
        {
            return new Constants.Value(fromString((String) parsed));
        }
        catch (ClassCastException exc)
        {
            throw new MarshalException(String.format(
                    "Expected a string representation of a uuid, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
        }
    }

    public TypeSerializer<UUID> getSerializer()
    {
        return UUIDSerializer.instance;
    }

    @Override
    protected int valueLengthIfFixed()
    {
        return 16;
    }
}
