import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.Roles;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.auth.*;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.RoleName;
import org.apache.cassandra.exceptions.*;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.transport.messages.ResultMessage;

public class DropRoleStatement extends AuthenticationStatement
{
    private final RoleResource role;
    private final boolean ifExists;

    public DropRoleStatement(RoleName name, boolean ifExists)
    {
        this.role = RoleResource.role(name.getName());
        this.ifExists = ifExists;
    }

    public void checkAccess(ClientState state) throws UnauthorizedException
    {
        super.checkPermission(state, Permission.DROP, role);

        // We only check superuser status for existing roles to avoid
        // caching info about roles which don't exist (CASSANDRA-9189)
        if (DatabaseDescriptor.getRoleManager().isExistingRole(role)
            && Roles.hasSuperuserStatus(role)
            && !state.getUser().isSuper())
            throw new UnauthorizedException("Only superusers can drop a role with superuser status");
    }

    public void validate(ClientState state) throws RequestValidationException
    {
        // validate login here before checkAccess to avoid leaking user existence to anonymous users.
        state.ensureNotAnonymous();

        if (!ifExists && !DatabaseDescriptor.getRoleManager().isExistingRole(role))
            throw new InvalidRequestException(String.format("%s doesn't exist", role.getRoleName()));

        AuthenticatedUser user = state.getUser();
        if (user != null && user.getName().equals(role.getRoleName()))
            throw new InvalidRequestException("Cannot DROP primary role for current login");
    }

    public ResultMessage execute(ClientState state) throws RequestValidationException, RequestExecutionException
    {
        // not rejected in validate()
        if (ifExists && !DatabaseDescriptor.getRoleManager().isExistingRole(role))
            return null;

        // clean up grants and permissions of/on the dropped role.
        DatabaseDescriptor.getRoleManager().dropRole(state.getUser(), role);
        DatabaseDescriptor.getAuthorizer().revokeAllFrom(role);
        DatabaseDescriptor.getAuthorizer().revokeAllOn(role);
        return null;
    }
}
