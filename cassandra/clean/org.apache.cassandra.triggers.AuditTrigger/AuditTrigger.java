import org.apache.cassandra.triggers.*;


import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.UUIDGen;

public class AuditTrigger implements ITrigger
{
    private Properties properties = loadProperties();

    public Collection<Mutation> augment(Partition update)
    {
        String auditKeyspace = properties.getProperty("keyspace");
        String auditTable = properties.getProperty("table");

        CFMetaData metadata = Schema.instance.getCFMetaData(auditKeyspace, auditTable);
        PartitionUpdate.SimpleBuilder audit = PartitionUpdate.simpleBuilder(metadata, UUIDGen.getTimeUUID());

        audit.row()
             .add("keyspace_name", update.metadata().ksName)
             .add("table_name", update.metadata().cfName)
             .add("primary_key", update.metadata().getKeyValidator().getString(update.partitionKey().getKey()));

        return Collections.singletonList(audit.buildAsMutation());
    }

    private static Properties loadProperties()
    {
        Properties properties = new Properties();
        InputStream stream = AuditTrigger.class.getClassLoader().getResourceAsStream("AuditTrigger.properties");
        try
        {
            properties.load(stream);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            FileUtils.closeQuietly(stream);
        }
        return properties;
    }
}
