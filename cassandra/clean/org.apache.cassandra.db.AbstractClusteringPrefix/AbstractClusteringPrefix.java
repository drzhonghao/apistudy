import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.*;


import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Objects;

import org.apache.cassandra.utils.FBUtilities;

public abstract class AbstractClusteringPrefix implements ClusteringPrefix
{
    public ClusteringPrefix clustering()
    {
        return this;
    }

    public int dataSize()
    {
        int size = 0;
        for (int i = 0; i < size(); i++)
        {
            ByteBuffer bb = get(i);
            size += bb == null ? 0 : bb.remaining();
        }
        return size;
    }

    public void digest(MessageDigest digest)
    {
        for (int i = 0; i < size(); i++)
        {
            ByteBuffer bb = get(i);
            if (bb != null)
                digest.update(bb.duplicate());
        }
        FBUtilities.updateWithByte(digest, kind().ordinal());
    }

    @Override
    public final int hashCode()
    {
        int result = 31;
        for (int i = 0; i < size(); i++)
            result += 31 * Objects.hashCode(get(i));
        return 31 * result + Objects.hashCode(kind());
    }

    @Override
    public final boolean equals(Object o)
    {
        if(!(o instanceof ClusteringPrefix))
            return false;

        ClusteringPrefix that = (ClusteringPrefix)o;
        if (this.kind() != that.kind() || this.size() != that.size())
            return false;

        for (int i = 0; i < size(); i++)
            if (!Objects.equals(this.get(i), that.get(i)))
                return false;

        return true;
    }
}
