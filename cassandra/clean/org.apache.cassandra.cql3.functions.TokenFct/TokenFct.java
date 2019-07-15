import org.apache.cassandra.cql3.functions.*;


import java.nio.ByteBuffer;
import java.util.List;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.CBuilder;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.transport.ProtocolVersion;

public class TokenFct extends NativeScalarFunction
{
    private final CFMetaData cfm;

    public TokenFct(CFMetaData cfm)
    {
        super("token", cfm.partitioner.getTokenValidator(), getKeyTypes(cfm));
        this.cfm = cfm;
    }

    private static AbstractType[] getKeyTypes(CFMetaData cfm)
    {
        AbstractType[] types = new AbstractType[cfm.partitionKeyColumns().size()];
        int i = 0;
        for (ColumnDefinition def : cfm.partitionKeyColumns())
            types[i++] = def.type;
        return types;
    }

    public ByteBuffer execute(ProtocolVersion protocolVersion, List<ByteBuffer> parameters) throws InvalidRequestException
    {
        CBuilder builder = CBuilder.create(cfm.getKeyValidatorAsClusteringComparator());
        for (int i = 0; i < parameters.size(); i++)
        {
            ByteBuffer bb = parameters.get(i);
            if (bb == null)
                return null;
            builder.add(bb);
        }
        return cfm.partitioner.getTokenFactory().toByteArray(cfm.partitioner.getToken(CFMetaData.serializePartitionKey(builder.build())));
    }
}
