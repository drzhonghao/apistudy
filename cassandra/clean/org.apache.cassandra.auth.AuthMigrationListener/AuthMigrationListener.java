import org.apache.cassandra.auth.DataResource;
import org.apache.cassandra.auth.FunctionResource;
import org.apache.cassandra.auth.*;


import java.util.List;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.service.MigrationListener;

/**
 * MigrationListener implementation that cleans up permissions on dropped resources.
 */
public class AuthMigrationListener extends MigrationListener
{
    public void onDropKeyspace(String ksName)
    {
        DatabaseDescriptor.getAuthorizer().revokeAllOn(DataResource.keyspace(ksName));
        DatabaseDescriptor.getAuthorizer().revokeAllOn(FunctionResource.keyspace(ksName));
    }

    public void onDropColumnFamily(String ksName, String cfName)
    {
        DatabaseDescriptor.getAuthorizer().revokeAllOn(DataResource.table(ksName, cfName));
    }

    public void onDropFunction(String ksName, String functionName, List<AbstractType<?>> argTypes)
    {
        DatabaseDescriptor.getAuthorizer()
                          .revokeAllOn(FunctionResource.function(ksName, functionName, argTypes));
    }

    public void onDropAggregate(String ksName, String aggregateName, List<AbstractType<?>> argTypes)
    {
        DatabaseDescriptor.getAuthorizer()
                          .revokeAllOn(FunctionResource.function(ksName, aggregateName, argTypes));
    }
}
