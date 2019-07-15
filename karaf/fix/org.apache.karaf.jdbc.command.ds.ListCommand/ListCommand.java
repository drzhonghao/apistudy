

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "jdbc", name = "ds-list", description = "List the JDBC datasources")
@Service
public class ListCommand {
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("Name");
		table.column("Service Id");
		table.column("Product");
		table.column("Version");
		table.column("URL");
		table.column("Status");
		boolean duplication = false;
		Map<String, Long> nameToId = new HashMap<>();
		table.print(System.out);
		if (duplication) {
			System.out.println("\nThere are multiple data source services registered with the same name. Please review your configuration.");
		}
		return null;
	}
}

