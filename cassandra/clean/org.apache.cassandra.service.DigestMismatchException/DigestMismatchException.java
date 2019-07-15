import org.apache.cassandra.service.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.utils.ByteBufferUtil;

@SuppressWarnings("serial")
public class DigestMismatchException extends Exception
{
    public DigestMismatchException(DecoratedKey key, ByteBuffer digest1, ByteBuffer digest2)
    {
        super(String.format("Mismatch for key %s (%s vs %s)",
                            key.toString(),
                            ByteBufferUtil.bytesToHex(digest1),
                            ByteBufferUtil.bytesToHex(digest2)));
    }
}
