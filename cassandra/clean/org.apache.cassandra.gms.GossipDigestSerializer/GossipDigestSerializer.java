import org.apache.cassandra.gms.*;


import java.io.*;
import java.net.InetAddress;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.CompactEndpointSerializationHelper;

/**
 * Contains information about a specified list of Endpoints and the largest version
 * of the state they have generated as known by the local endpoint.
 */
public class GossipDigest implements Comparable<GossipDigest>
{
    public static final IVersionedSerializer<GossipDigest> serializer = new GossipDigestSerializer();

    final InetAddress endpoint;
    final int generation;
    final int maxVersion;

    GossipDigest(InetAddress ep, int gen, int version)
    {
        endpoint = ep;
        generation = gen;
        maxVersion = version;
    }

    InetAddress getEndpoint()
    {
        return endpoint;
    }

    int getGeneration()
    {
        return generation;
    }

    int getMaxVersion()
    {
        return maxVersion;
    }

    public int compareTo(GossipDigest gDigest)
    {
        if (generation != gDigest.generation)
            return (generation - gDigest.generation);
        return (maxVersion - gDigest.maxVersion);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(endpoint);
        sb.append(":");
        sb.append(generation);
        sb.append(":");
        sb.append(maxVersion);
        return sb.toString();
    }
}

class GossipDigestSerializer implements IVersionedSerializer<GossipDigest>
{
    public void serialize(GossipDigest gDigest, DataOutputPlus out, int version) throws IOException
    {
        CompactEndpointSerializationHelper.serialize(gDigest.endpoint, out);
        out.writeInt(gDigest.generation);
        out.writeInt(gDigest.maxVersion);
    }

    public GossipDigest deserialize(DataInputPlus in, int version) throws IOException
    {
        InetAddress endpoint = CompactEndpointSerializationHelper.deserialize(in);
        int generation = in.readInt();
        int maxVersion = in.readInt();
        return new GossipDigest(endpoint, generation, maxVersion);
    }

    public long serializedSize(GossipDigest gDigest, int version)
    {
        long size = CompactEndpointSerializationHelper.serializedSize(gDigest.endpoint);
        size += TypeSizes.sizeof(gDigest.generation);
        size += TypeSizes.sizeof(gDigest.maxVersion);
        return size;
    }
}
