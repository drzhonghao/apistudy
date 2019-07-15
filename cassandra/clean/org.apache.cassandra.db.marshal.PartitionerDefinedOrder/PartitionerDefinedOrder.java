import org.apache.cassandra.db.marshal.*;


import java.nio.ByteBuffer;
import java.util.Iterator;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

/** for sorting columns representing row keys in the row ordering as determined by a partitioner.
 * Not intended for user-defined CFs, and will in fact error out if used with such. */
public class PartitionerDefinedOrder extends AbstractType<ByteBuffer>
{
    private final IPartitioner partitioner;

    public PartitionerDefinedOrder(IPartitioner partitioner)
    {
        super(ComparisonType.CUSTOM);
        this.partitioner = partitioner;
    }

    public static AbstractType<?> getInstance(TypeParser parser)
    {
        IPartitioner partitioner = DatabaseDescriptor.getPartitioner();
        Iterator<String> argIterator = parser.getKeyValueParameters().keySet().iterator();
        if (argIterator.hasNext())
        {
            partitioner = FBUtilities.newPartitioner(argIterator.next());
            assert !argIterator.hasNext();
        }
        return partitioner.partitionOrdering();
    }

    @Override
    public ByteBuffer compose(ByteBuffer bytes)
    {
        throw new UnsupportedOperationException("You can't do this with a local partitioner.");
    }

    @Override
    public ByteBuffer decompose(ByteBuffer bytes)
    {
        throw new UnsupportedOperationException("You can't do this with a local partitioner.");
    }

    public String getString(ByteBuffer bytes)
    {
        return ByteBufferUtil.bytesToHex(bytes);
    }

    public ByteBuffer fromString(String source)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term fromJSONObject(Object parsed)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        throw new UnsupportedOperationException();
    }

    public int compareCustom(ByteBuffer o1, ByteBuffer o2)
    {
        // o1 and o2 can be empty so we need to use PartitionPosition, not DecoratedKey
        return PartitionPosition.ForKey.get(o1, partitioner).compareTo(PartitionPosition.ForKey.get(o2, partitioner));
    }

    @Override
    public void validate(ByteBuffer bytes) throws MarshalException
    {
        throw new IllegalStateException("You shouldn't be validating this.");
    }

    public TypeSerializer<ByteBuffer> getSerializer()
    {
        throw new UnsupportedOperationException("You can't do this with a local partitioner.");
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s)", getClass().getName(), partitioner.getClass().getName());
    }
}
