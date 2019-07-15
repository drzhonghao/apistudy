import org.apache.cassandra.db.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Exception thrown when we read a column internally that is unknown. Note that
 * this is an internal exception and is not meant to be user facing.
 */
public class UnknownColumnException extends Exception
{
    public final ByteBuffer columnName;

    public UnknownColumnException(CFMetaData metadata, ByteBuffer columnName)
    {
        super(String.format("Unknown column %s in table %s.%s", stringify(columnName), metadata.ksName, metadata.cfName));
        this.columnName = columnName;
    }

    private static String stringify(ByteBuffer name)
    {
        try
        {
            return UTF8Type.instance.getString(name);
        }
        catch (Exception e)
        {
            return ByteBufferUtil.bytesToHex(name);
        }
    }
}
