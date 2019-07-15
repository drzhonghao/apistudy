import org.apache.cassandra.auth.IAuthorizer;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.PermissionDetails;
import org.apache.cassandra.auth.*;


import java.util.Collections;
import java.util.Set;

public class AllowAllAuthorizer implements IAuthorizer
{
    @Override
    public boolean requireAuthorization()
    {
        return false;
    }

    public Set<Permission> authorize(AuthenticatedUser user, IResource resource)
    {
        return resource.applicablePermissions();
    }

    public void grant(AuthenticatedUser performer, Set<Permission> permissions, IResource resource, RoleResource to)
    {
        throw new UnsupportedOperationException("GRANT operation is not supported by AllowAllAuthorizer");
    }

    public void revoke(AuthenticatedUser performer, Set<Permission> permissions, IResource resource, RoleResource from)
    {
        throw new UnsupportedOperationException("REVOKE operation is not supported by AllowAllAuthorizer");
    }

    public void revokeAllFrom(RoleResource droppedRole)
    {
    }

    public void revokeAllOn(IResource droppedResource)
    {
    }

    public Set<PermissionDetails> list(AuthenticatedUser performer, Set<Permission> permissions, IResource resource, RoleResource of)
    {
        throw new UnsupportedOperationException("LIST PERMISSIONS operation is not supported by AllowAllAuthorizer");
    }

    public Set<IResource> protectedResources()
    {
        return Collections.emptySet();
    }

    public void validateConfiguration()
    {
    }

    public void setup()
    {
    }
}
