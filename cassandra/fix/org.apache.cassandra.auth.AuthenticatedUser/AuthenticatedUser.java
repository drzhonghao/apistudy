

import com.google.common.base.Objects;
import java.util.Set;
import org.apache.cassandra.auth.IAuthorizer;
import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.auth.PermissionsCache;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.Roles;
import org.apache.cassandra.config.DatabaseDescriptor;


public class AuthenticatedUser {
	public static final String SYSTEM_USERNAME = "system";

	public static final AuthenticatedUser SYSTEM_USER = new AuthenticatedUser(AuthenticatedUser.SYSTEM_USERNAME);

	public static final String ANONYMOUS_USERNAME = "anonymous";

	public static final AuthenticatedUser ANONYMOUS_USER = new AuthenticatedUser(AuthenticatedUser.ANONYMOUS_USERNAME);

	private static final PermissionsCache permissionsCache = new PermissionsCache(DatabaseDescriptor.getAuthorizer());

	private final String name;

	private final RoleResource role;

	public AuthenticatedUser(String name) {
		this.name = name;
		this.role = RoleResource.role(name);
	}

	public String getName() {
		return name;
	}

	public RoleResource getPrimaryRole() {
		return role;
	}

	public boolean isSuper() {
		return (!(isAnonymous())) && (Roles.hasSuperuserStatus(role));
	}

	public boolean isAnonymous() {
		return (this) == (AuthenticatedUser.ANONYMOUS_USER);
	}

	public boolean isSystem() {
		return (this) == (AuthenticatedUser.SYSTEM_USER);
	}

	public Set<RoleResource> getRoles() {
		return Roles.getRoles(role);
	}

	public Set<Permission> getPermissions(IResource resource) {
		return null;
	}

	@Override
	public String toString() {
		return String.format("#<User %s>", name);
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof AuthenticatedUser))
			return false;

		AuthenticatedUser u = ((AuthenticatedUser) (o));
		return Objects.equal(name, u.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}
}

