import org.apache.cassandra.io.sstable.metadata.*;


/**
 * MetadataComponent is a component for SSTable metadata and serialized to Stats.db.
 */
public abstract class MetadataComponent implements Comparable<MetadataComponent>
{
    /**
     * @return Metadata component type
     */
    public abstract MetadataType getType();

    public int compareTo(MetadataComponent o)
    {
        return this.getType().compareTo(o.getType());
    }
}
