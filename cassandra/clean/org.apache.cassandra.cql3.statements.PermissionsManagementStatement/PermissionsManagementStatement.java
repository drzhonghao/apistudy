import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.FunctionResource;
import org.apache.cassandra.cql3.statements.*;


import java.util.Set;

import org.apache.cassandra.auth.*;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.RoleName;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.service.ClientState;

public abstract class PermissionsManagementStatement extends AuthorizationStatement
{
    protected final Set<Permission> permissions;
    protected IResource resource;
    protected final RoleResource grantee;

    protected PermissionsManagementStatement(Set<Permission> permissions, IResource resource, RoleName grantee)
    {
        this.permissions = permissions;
        this.resource = resource;
        this.grantee = RoleResource.role(grantee.getName());
    }

    public void validate(ClientState state) throws RequestValidationException
    {
        // validate login here before checkAccess to avoid leaking user existence to anonymous users.
        state.ensureNotAnonymous();

        if (!DatabaseDescriptor.getRoleManager().isExistingRole(grantee))
            throw new InvalidRequestException(String.format("Role %s doesn't exist", grantee.getRoleName()));

        // if a keyspace is omitted when GRANT/REVOKE ON TABLE <table>, we need to correct the resource.
        // called both here and in checkAccess(), as in some cases we do not call the latter.
        resource = maybeCorrectResource(resource, state);

        // altering permissions on builtin functions is not supported
        if (resource instanceof FunctionResource
            && SchemaConstants.SYSTEM_KEYSPACE_NAME.equals(((FunctionResource)resource).getKeyspace()))
        {
            throw new InvalidRequestException("Altering permissions on builtin functions is not supported");
        }

        if (!resource.exists())
            throw new InvalidRequestException(String.format("Resource %s doesn't exist", resource));
    }

    public void checkAccess(ClientState state) throws UnauthorizedException
    {
        // if a keyspace is omitted when GRANT/REVOKE ON TABLE <table>, we need to correct the resource.
        resource = maybeCorrectResource(resource, state);

        // check that the user has AUTHORIZE permission on the resource or its parents, otherwise reject GRANT/REVOKE.
        state.ensureHasPermission(Permission.AUTHORIZE, resource);

        // check that the user has [a single permission or all in case of ALL] on the resource or its parents.
        for (Permission p : permissions)
            state.ensureHasPermission(p, resource);
    }
}
