import org.apache.cassandra.db.filter.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.marshal.*;

public class TombstoneOverwhelmingException extends RuntimeException
{
    public TombstoneOverwhelmingException(int numTombstones, String query, CFMetaData metadata, DecoratedKey lastPartitionKey, ClusteringPrefix lastClustering)
    {
        super(String.format("Scanned over %d tombstones during query '%s' (last scanned row partion key was (%s)); query aborted",
                            numTombstones, query, makePKString(metadata, lastPartitionKey.getKey(), lastClustering)));
    }

    private static String makePKString(CFMetaData metadata, ByteBuffer partitionKey, ClusteringPrefix clustering)
    {
        StringBuilder sb = new StringBuilder();

        if (clustering.size() > 0)
            sb.append("(");

        // TODO: We should probably make that a lot easier/transparent for partition keys
        AbstractType<?> pkType = metadata.getKeyValidator();
        if (pkType instanceof CompositeType)
        {
            CompositeType ct = (CompositeType)pkType;
            ByteBuffer[] values = ct.split(partitionKey);
            for (int i = 0; i < values.length; i++)
            {
                if (i > 0)
                    sb.append(", ");
                sb.append(ct.types.get(i).getString(values[i]));
            }
        }
        else
        {
            sb.append(pkType.getString(partitionKey));
        }

        if (clustering.size() > 0)
            sb.append(")");

        for (int i = 0; i < clustering.size(); i++)
            sb.append(", ").append(metadata.comparator.subtype(i).getString(clustering.get(i)));

        return sb.toString();
    }
}
