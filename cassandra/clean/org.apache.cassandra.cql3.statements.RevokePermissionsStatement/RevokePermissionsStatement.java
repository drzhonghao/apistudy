import org.apache.cassandra.cql3.statements.*;


import java.util.Set;

import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.RoleName;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.transport.messages.ResultMessage;

public class RevokePermissionsStatement extends PermissionsManagementStatement
{
    public RevokePermissionsStatement(Set<Permission> permissions, IResource resource, RoleName grantee)
    {
        super(permissions, resource, grantee);
    }

    public ResultMessage execute(ClientState state) throws RequestValidationException, RequestExecutionException
    {
        DatabaseDescriptor.getAuthorizer().revoke(state.getUser(), permissions, resource, grantee);
        return null;
    }
}
