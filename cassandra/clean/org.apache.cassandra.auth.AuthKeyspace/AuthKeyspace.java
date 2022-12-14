import org.apache.cassandra.auth.*;


import java.util.concurrent.TimeUnit;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.Tables;

public final class AuthKeyspace
{
    private AuthKeyspace()
    {
    }

    public static final String ROLES = "roles";
    public static final String ROLE_MEMBERS = "role_members";
    public static final String ROLE_PERMISSIONS = "role_permissions";
    public static final String RESOURCE_ROLE_INDEX = "resource_role_permissons_index";

    public static final long SUPERUSER_SETUP_DELAY = Long.getLong("cassandra.superuser_setup_delay_ms", 10000);

    private static final CFMetaData Roles =
        compile(ROLES,
                "role definitions",
                "CREATE TABLE %s ("
                + "role text,"
                + "is_superuser boolean,"
                + "can_login boolean,"
                + "salted_hash text,"
                + "member_of set<text>,"
                + "PRIMARY KEY(role))");

    private static final CFMetaData RoleMembers =
        compile(ROLE_MEMBERS,
                "role memberships lookup table",
                "CREATE TABLE %s ("
                + "role text,"
                + "member text,"
                + "PRIMARY KEY(role, member))");

    private static final CFMetaData RolePermissions =
        compile(ROLE_PERMISSIONS,
                "permissions granted to db roles",
                "CREATE TABLE %s ("
                + "role text,"
                + "resource text,"
                + "permissions set<text>,"
                + "PRIMARY KEY(role, resource))");

    private static final CFMetaData ResourceRoleIndex =
        compile(RESOURCE_ROLE_INDEX,
                "index of db roles with permissions granted on a resource",
                "CREATE TABLE %s ("
                + "resource text,"
                + "role text,"
                + "PRIMARY KEY(resource, role))");


    private static CFMetaData compile(String name, String description, String schema)
    {
        return CFMetaData.compile(String.format(schema, name), SchemaConstants.AUTH_KEYSPACE_NAME)
                         .comment(description)
                         .gcGraceSeconds((int) TimeUnit.DAYS.toSeconds(90));
    }

    public static KeyspaceMetadata metadata()
    {
        return KeyspaceMetadata.create(SchemaConstants.AUTH_KEYSPACE_NAME, KeyspaceParams.simple(1), Tables.of(Roles, RoleMembers, RolePermissions, ResourceRoleIndex));
    }
}
