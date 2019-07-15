

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.Permission;
import org.apache.commons.lang3.StringUtils;


public class RoleResource implements Comparable<RoleResource> , IResource {
	enum Level {

		ROOT,
		ROLE;}

	private static final Set<Permission> ROOT_LEVEL_PERMISSIONS = Sets.immutableEnumSet(Permission.CREATE, Permission.ALTER, Permission.DROP, Permission.AUTHORIZE, Permission.DESCRIBE);

	private static final Set<Permission> ROLE_LEVEL_PERMISSIONS = Sets.immutableEnumSet(Permission.ALTER, Permission.DROP, Permission.AUTHORIZE);

	private static final String ROOT_NAME = "roles";

	private static final RoleResource ROOT_RESOURCE = new RoleResource();

	private final RoleResource.Level level;

	private final String name;

	private RoleResource() {
		level = RoleResource.Level.ROOT;
		name = null;
	}

	private RoleResource(String name) {
		level = RoleResource.Level.ROLE;
		this.name = name;
	}

	public static RoleResource root() {
		return RoleResource.ROOT_RESOURCE;
	}

	public static RoleResource role(String name) {
		return new RoleResource(name);
	}

	public static RoleResource fromName(String name) {
		String[] parts = StringUtils.split(name, "/", 2);
		if (!(parts[0].equals(RoleResource.ROOT_NAME)))
			throw new IllegalArgumentException(String.format("%s is not a valid role resource name", name));

		if ((parts.length) == 1)
			return RoleResource.root();

		return RoleResource.role(parts[1]);
	}

	public String getName() {
		return (level) == (RoleResource.Level.ROOT) ? RoleResource.ROOT_NAME : String.format("%s/%s", RoleResource.ROOT_NAME, name);
	}

	public String getRoleName() {
		if ((level) == (RoleResource.Level.ROOT))
			throw new IllegalStateException(String.format("%s role resource has no role name", level));

		return name;
	}

	public IResource getParent() {
		if ((level) == (RoleResource.Level.ROLE))
			return RoleResource.root();

		throw new IllegalStateException("Root-level resource can't have a parent");
	}

	public boolean hasParent() {
		return (level) != (RoleResource.Level.ROOT);
	}

	public boolean exists() {
		return false;
	}

	public Set<Permission> applicablePermissions() {
		return (level) == (RoleResource.Level.ROOT) ? RoleResource.ROOT_LEVEL_PERMISSIONS : RoleResource.ROLE_LEVEL_PERMISSIONS;
	}

	public int compareTo(RoleResource o) {
		return this.name.compareTo(o.name);
	}

	@Override
	public String toString() {
		return (level) == (RoleResource.Level.ROOT) ? "<all roles>" : String.format("<role %s>", name);
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof RoleResource))
			return false;

		RoleResource rs = ((RoleResource) (o));
		return (Objects.equal(level, rs.level)) && (Objects.equal(name, rs.name));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(level, name);
	}
}

