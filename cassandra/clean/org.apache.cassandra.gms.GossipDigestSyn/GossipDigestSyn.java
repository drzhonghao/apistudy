import org.apache.cassandra.gms.GossipDigest;
import org.apache.cassandra.gms.*;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;

/**
 * This is the first message that gets sent out as a start of the Gossip protocol in a
 * round.
 */
public class GossipDigestSyn
{
    public static final IVersionedSerializer<GossipDigestSyn> serializer = new GossipDigestSynSerializer();

    final String clusterId;
    final String partioner;
    final List<GossipDigest> gDigests;

    public GossipDigestSyn(String clusterId, String partioner, List<GossipDigest> gDigests)
    {
        this.clusterId = clusterId;
        this.partioner = partioner;
        this.gDigests = gDigests;
    }

    List<GossipDigest> getGossipDigests()
    {
        return gDigests;
    }
}

class GossipDigestSerializationHelper
{
    static void serialize(List<GossipDigest> gDigestList, DataOutputPlus out, int version) throws IOException
    {
        out.writeInt(gDigestList.size());
        for (GossipDigest gDigest : gDigestList)
            GossipDigest.serializer.serialize(gDigest, out, version);
    }

    static List<GossipDigest> deserialize(DataInputPlus in, int version) throws IOException
    {
        int size = in.readInt();
        List<GossipDigest> gDigests = new ArrayList<GossipDigest>(size);
        for (int i = 0; i < size; ++i)
            gDigests.add(GossipDigest.serializer.deserialize(in, version));
        return gDigests;
    }

    static int serializedSize(List<GossipDigest> digests, int version)
    {
        int size = TypeSizes.sizeof(digests.size());
        for (GossipDigest digest : digests)
            size += GossipDigest.serializer.serializedSize(digest, version);
        return size;
    }
}

class GossipDigestSynSerializer implements IVersionedSerializer<GossipDigestSyn>
{
    public void serialize(GossipDigestSyn gDigestSynMessage, DataOutputPlus out, int version) throws IOException
    {
        out.writeUTF(gDigestSynMessage.clusterId);
        out.writeUTF(gDigestSynMessage.partioner);
        GossipDigestSerializationHelper.serialize(gDigestSynMessage.gDigests, out, version);
    }

    public GossipDigestSyn deserialize(DataInputPlus in, int version) throws IOException
    {
        String clusterId = in.readUTF();
        String partioner = null;
        partioner = in.readUTF();
        List<GossipDigest> gDigests = GossipDigestSerializationHelper.deserialize(in, version);
        return new GossipDigestSyn(clusterId, partioner, gDigests);
    }

    public long serializedSize(GossipDigestSyn syn, int version)
    {
        long size = TypeSizes.sizeof(syn.clusterId);
        size += TypeSizes.sizeof(syn.partioner);
        size += GossipDigestSerializationHelper.serializedSize(syn.gDigests, version);
        return size;
    }
}
