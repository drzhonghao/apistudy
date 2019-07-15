import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.IRoleManager;
import org.apache.cassandra.auth.Roles;
import org.apache.cassandra.cql3.statements.*;


import java.util.List;

import com.google.common.collect.ImmutableList;

import org.apache.cassandra.auth.*;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.ResultSet;
import org.apache.cassandra.db.marshal.BooleanType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.transport.messages.ResultMessage;

public class ListUsersStatement extends ListRolesStatement
{
    // pseudo-virtual cf as the actual datasource is dependent on the IRoleManager impl
    private static final String KS = SchemaConstants.AUTH_KEYSPACE_NAME;
    private static final String CF = "users";

    private static final List<ColumnSpecification> metadata =
        ImmutableList.of(new ColumnSpecification(KS, CF, new ColumnIdentifier("name", true), UTF8Type.instance),
                         new ColumnSpecification(KS, CF, new ColumnIdentifier("super", true), BooleanType.instance));

    @Override
    protected ResultMessage formatResults(List<RoleResource> sortedRoles)
    {
        ResultSet result = new ResultSet(metadata);

        IRoleManager roleManager = DatabaseDescriptor.getRoleManager();
        for (RoleResource role : sortedRoles)
        {
            if (!roleManager.canLogin(role))
                continue;
            result.addColumnValue(UTF8Type.instance.decompose(role.getRoleName()));
            result.addColumnValue(BooleanType.instance.decompose(Roles.hasSuperuserStatus(role)));
        }
        return new ResultMessage.Rows(result);
    }
}
