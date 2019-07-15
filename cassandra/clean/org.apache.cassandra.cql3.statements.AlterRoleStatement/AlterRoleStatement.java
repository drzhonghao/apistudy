import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.RoleOptions;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.auth.*;
import org.apache.cassandra.auth.IRoleManager.Option;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.RoleName;
import org.apache.cassandra.exceptions.*;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.transport.messages.ResultMessage;

public class AlterRoleStatement extends AuthenticationStatement
{
    private final RoleResource role;
    private final RoleOptions opts;

    public AlterRoleStatement(RoleName name, RoleOptions opts)
    {
        this.role = RoleResource.role(name.getName());
        this.opts = opts;
    }

    public void validate(ClientState state) throws RequestValidationException
    {
        opts.validate();

        if (opts.isEmpty())
            throw new InvalidRequestException("ALTER [ROLE|USER] can't be empty");

        // validate login here before checkAccess to avoid leaking user existence to anonymous users.
        state.ensureNotAnonymous();
        if (!DatabaseDescriptor.getRoleManager().isExistingRole(role))
            throw new InvalidRequestException(String.format("%s doesn't exist", role.getRoleName()));
    }

    public void checkAccess(ClientState state) throws UnauthorizedException
    {
        AuthenticatedUser user = state.getUser();
        boolean isSuper = user.isSuper();

        if (opts.getSuperuser().isPresent() && user.getRoles().contains(role))
            throw new UnauthorizedException("You aren't allowed to alter your own superuser " +
                                            "status or that of a role granted to you");

        if (opts.getSuperuser().isPresent() && !isSuper)
            throw new UnauthorizedException("Only superusers are allowed to alter superuser status");

        // superusers can do whatever else they like
        if (isSuper)
            return;

        // a role may only modify the subset of its own attributes as determined by IRoleManager#alterableOptions
        if (user.getName().equals(role.getRoleName()))
        {
            for (Option option : opts.getOptions().keySet())
            {
                if (!DatabaseDescriptor.getRoleManager().alterableOptions().contains(option))
                    throw new UnauthorizedException(String.format("You aren't allowed to alter %s", option));
            }
        }
        else
        {
            // if not attempting to alter another role, ensure we have ALTER permissions on it
            super.checkPermission(state, Permission.ALTER, role);
        }
    }

    public ResultMessage execute(ClientState state) throws RequestValidationException, RequestExecutionException
    {
        if (!opts.isEmpty())
            DatabaseDescriptor.getRoleManager().alterRole(state.getUser(), role, opts);
        return null;
    }
}
