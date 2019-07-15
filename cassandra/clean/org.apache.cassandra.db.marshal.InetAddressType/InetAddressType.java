import org.apache.cassandra.db.marshal.*;


import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.InetAddressSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

public class InetAddressType extends AbstractType<InetAddress>
{
    public static final InetAddressType instance = new InetAddressType();

    InetAddressType() {super(ComparisonType.BYTE_ORDER);} // singleton

    public boolean isEmptyValueMeaningless()
    {
        return true;
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
        // Return an empty ByteBuffer for an empty string.
        if (source.isEmpty())
            return ByteBufferUtil.EMPTY_BYTE_BUFFER;

        InetAddress address;

        try
        {
            address = InetAddress.getByName(source);
        }
        catch (Exception e)
        {
            throw new MarshalException(String.format("Unable to make inet address from '%s'", source), e);
        }

        return decompose(address);
    }

    @Override
    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        try
        {
            return new Constants.Value(InetAddressType.instance.fromString((String) parsed));
        }
        catch (ClassCastException exc)
        {
            throw new MarshalException(String.format(
                    "Expected a string representation of an inet value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
        }
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return '"' + getSerializer().deserialize(buffer).getHostAddress() + '"';
    }

    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.INET;
    }

    public TypeSerializer<InetAddress> getSerializer()
    {
        return InetAddressSerializer.instance;
    }
}
