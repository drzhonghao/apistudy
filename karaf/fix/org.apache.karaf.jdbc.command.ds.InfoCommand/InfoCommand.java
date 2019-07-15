

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "jdbc", name = "ds-info", description = "Display details about a JDBC datasource")
@Service
public class InfoCommand {
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("Property");
		table.column("Value");
		table.print(System.out);
		return null;
	}
}

