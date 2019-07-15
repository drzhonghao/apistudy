import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.RoleName;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.transport.messages.ResultMessage;

public class GrantRoleStatement extends RoleManagementStatement
{
    public GrantRoleStatement(RoleName name, RoleName grantee)
    {
        super(name, grantee);
    }

    public ResultMessage execute(ClientState state) throws RequestValidationException, RequestExecutionException
    {
        DatabaseDescriptor.getRoleManager().grantRole(state.getUser(), role, grantee);
        return null;
    }
}
