

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "docker", name = "ps", description = "List containers")
@Service
public class PsCommand {
	@Option(name = "-a", aliases = { "--all", "--showAll" }, description = "Display all containers or only running ones", required = false, multiValued = false)
	boolean showAll;

	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("Id");
		table.column("Names");
		table.column("Command");
		table.column("Created");
		table.column("Image");
		table.column("Image ID");
		table.column("Status");
		table.column("State");
		table.column("Ports");
		table.column("Size");
		table.column("Size Root");
		table.print(System.out);
		return null;
	}
}

