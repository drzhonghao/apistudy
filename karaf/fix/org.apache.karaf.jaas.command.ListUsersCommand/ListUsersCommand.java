

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.command.JaasCommandSupport;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "jaas", name = "user-list", description = "List the users of the selected JAAS realm/login module")
@Service
public class ListUsersCommand extends JaasCommandSupport {
	@Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
	boolean noFormat;

	@Override
	public Object execute() throws Exception {
		return null;
	}

	@Override
	protected Object doExecute(BackingEngine engine) throws Exception {
		List<UserPrincipal> users = engine.listUsers();
		ShellTable table = new ShellTable();
		table.column("User Name");
		table.column("Group");
		table.column("Role");
		for (UserPrincipal user : users) {
			List<String> reportedRoles = new ArrayList<>();
			String userName = user.getName();
			for (GroupPrincipal group : engine.listGroups(user)) {
				reportedRoles.addAll(displayGroupRoles(engine, userName, group, table));
			}
			for (RolePrincipal role : engine.listRoles(user)) {
				String roleName = role.getName();
				if (reportedRoles.contains(roleName)) {
					continue;
				}
				reportedRoles.add(roleName);
				table.addRow().addContent(userName, "", roleName);
			}
			if ((reportedRoles.size()) == 0) {
				table.addRow().addContent(userName, "", "");
			}
		}
		table.print(System.out, (!(noFormat)));
		return null;
	}

	private List<String> displayGroupRoles(BackingEngine engine, String userName, GroupPrincipal group, ShellTable table) {
		List<String> names = new ArrayList<>();
		List<RolePrincipal> roles = engine.listRoles(group);
		if ((roles != null) && ((roles.size()) >= 1)) {
			for (RolePrincipal role : roles) {
				String roleName = role.getName();
				names.add(roleName);
				table.addRow().addContent(userName, group.getName(), roleName);
			}
		}
		return names;
	}
}

