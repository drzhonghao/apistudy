

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "docker", name = "history", description = "Show the history of an image")
@Service
public class HistoryCommand {
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("ID");
		table.column("Created");
		table.column("Created By");
		table.column("Tags");
		table.column("Size");
		table.print(System.out);
		return null;
	}
}

