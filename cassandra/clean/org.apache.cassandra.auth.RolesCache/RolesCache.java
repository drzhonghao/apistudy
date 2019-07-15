import org.apache.cassandra.auth.AuthCache;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.RolesCacheMBean;
import org.apache.cassandra.auth.IRoleManager;
import org.apache.cassandra.auth.*;


import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.cassandra.config.DatabaseDescriptor;

public class RolesCache extends AuthCache<RoleResource, Set<RoleResource>> implements RolesCacheMBean
{
    public RolesCache(IRoleManager roleManager)
    {
        super("RolesCache",
              DatabaseDescriptor::setRolesValidity,
              DatabaseDescriptor::getRolesValidity,
              DatabaseDescriptor::setRolesUpdateInterval,
              DatabaseDescriptor::getRolesUpdateInterval,
              DatabaseDescriptor::setRolesCacheMaxEntries,
              DatabaseDescriptor::getRolesCacheMaxEntries,
              (r) -> roleManager.getRoles(r, true),
              () -> DatabaseDescriptor.getAuthenticator().requireAuthentication());
    }

    public Set<RoleResource> getRoles(RoleResource role)
    {
        try
        {
            return get(role);
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }
}
