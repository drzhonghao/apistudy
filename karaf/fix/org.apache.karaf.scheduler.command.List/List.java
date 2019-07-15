

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "scheduler", name = "list", description = "List scheduled jobs")
@Service
public class List implements Action {
	@Override
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("Name");
		table.column("Schedule");
		table.print(System.out);
		return null;
	}
}

