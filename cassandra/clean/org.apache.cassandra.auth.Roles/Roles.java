import org.apache.cassandra.auth.RolesCache;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.IRoleManager;
import org.apache.cassandra.auth.*;


import java.util.Set;

import org.apache.cassandra.config.DatabaseDescriptor;

public class Roles
{
    private static final RolesCache cache = new RolesCache(DatabaseDescriptor.getRoleManager());

    /**
     * Get all roles granted to the supplied Role, including both directly granted
     * and inherited roles.
     * The returned roles may be cached if {@code roles_validity_in_ms > 0}
     *
     * @param primaryRole the Role
     * @return set of all granted Roles for the primary Role
     */
    public static Set<RoleResource> getRoles(RoleResource primaryRole)
    {
        return cache.getRoles(primaryRole);
    }

    /**
     * Returns true if the supplied role or any other role granted to it
     * (directly or indirectly) has superuser status.
     *
     * @param role the primary role
     * @return true if the role has superuser status, false otherwise
     */
    public static boolean hasSuperuserStatus(RoleResource role)
    {
        IRoleManager roleManager = DatabaseDescriptor.getRoleManager();
        for (RoleResource r : cache.getRoles(role))
            if (roleManager.isSuper(r))
                return true;
        return false;
    }
}
