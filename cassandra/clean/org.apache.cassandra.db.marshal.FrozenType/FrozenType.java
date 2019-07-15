import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;
import java.util.List;

import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.transport.ProtocolVersion;

/**
 * A fake type that is only used for parsing type strings that include frozen types.
 */
public class FrozenType extends AbstractType<Void>
{
    protected FrozenType()
    {
        super(ComparisonType.NOT_COMPARABLE);
    }

    public static AbstractType<?> getInstance(TypeParser parser) throws ConfigurationException, SyntaxException
    {
        List<AbstractType<?>> innerTypes = parser.getTypeParameters();
        if (innerTypes.size() != 1)
            throw new SyntaxException("FrozenType() only accepts one parameter");

        AbstractType<?> innerType = innerTypes.get(0);
        return innerType.freeze();
    }

    public String getString(ByteBuffer bytes)
    {
        throw new UnsupportedOperationException();
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
        throw new UnsupportedOperationException();
    }

    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        throw new UnsupportedOperationException();
    }

    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        throw new UnsupportedOperationException();
    }

    public TypeSerializer<Void> getSerializer()
    {
        throw new UnsupportedOperationException();
    }
}
