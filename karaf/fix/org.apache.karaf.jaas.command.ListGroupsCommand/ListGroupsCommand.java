

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.command.JaasCommandSupport;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jaas", name = "group-list", description = "List groups in a realm")
@Service
public class ListGroupsCommand extends JaasCommandSupport {
	private static final String GROUP_LIST_FORMAT = "%-10s  %-80s";

	@Override
	public Object execute() throws Exception {
		return null;
	}

	@Override
	protected Object doExecute(BackingEngine engine) throws Exception {
		System.out.println(String.format(ListGroupsCommand.GROUP_LIST_FORMAT, "Group", "Roles"));
		for (GroupPrincipal group : engine.listGroups().keySet()) {
			System.out.println(String.format(ListGroupsCommand.GROUP_LIST_FORMAT, group.getName(), engine.listGroups().get(group)));
		}
		return null;
	}
}

