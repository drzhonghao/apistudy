import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.*;


import java.nio.ByteBuffer;

public class UTName
{
    private String ksName;
    private final ColumnIdentifier utName;

    public UTName(ColumnIdentifier ksName, ColumnIdentifier utName)
    {
        this.ksName = ksName == null ? null : ksName.toString();
        this.utName = utName;
    }

    public boolean hasKeyspace()
    {
        return ksName != null;
    }

    public void setKeyspace(String keyspace)
    {
        this.ksName = keyspace;
    }

    public String getKeyspace()
    {
        return ksName;
    }

    public ByteBuffer getUserTypeName()
    {
        return utName.bytes;
    }

    public String getStringTypeName()
    {
        return utName.toString();
    }

    @Override
    public String toString()
    {
        return (hasKeyspace() ? (ksName + ".") : "") + utName;
    }
}
