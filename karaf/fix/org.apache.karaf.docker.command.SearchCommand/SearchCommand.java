

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "docker", name = "search", description = "Search the Docker Hub for images")
@Service
public class SearchCommand {
	@Argument(index = 0, name = "term", description = "Search term", required = true, multiValued = false)
	String term;

	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("Name");
		table.column("Description");
		table.column("Automated");
		table.column("Official");
		table.column("Star Count");
		table.print(System.out);
		return null;
	}
}

