import org.apache.cassandra.auth.AuthCache;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.PermissionsCacheMBean;
import org.apache.cassandra.auth.IAuthorizer;
import org.apache.cassandra.auth.*;


import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.utils.Pair;

public class PermissionsCache extends AuthCache<Pair<AuthenticatedUser, IResource>, Set<Permission>> implements PermissionsCacheMBean
{
    public PermissionsCache(IAuthorizer authorizer)
    {
        super("PermissionsCache",
              DatabaseDescriptor::setPermissionsValidity,
              DatabaseDescriptor::getPermissionsValidity,
              DatabaseDescriptor::setPermissionsUpdateInterval,
              DatabaseDescriptor::getPermissionsUpdateInterval,
              DatabaseDescriptor::setPermissionsCacheMaxEntries,
              DatabaseDescriptor::getPermissionsCacheMaxEntries,
              (p) -> authorizer.authorize(p.left, p.right),
              () -> DatabaseDescriptor.getAuthorizer().requireAuthorization());
    }

    public Set<Permission> getPermissions(AuthenticatedUser user, IResource resource)
    {
        try
        {
            return get(Pair.create(user, resource));
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }
}
