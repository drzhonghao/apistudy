

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "docker", name = "images", description = "List Docker images")
@Service
public class ImagesCommand {
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("Id");
		table.column("RepoTags");
		table.column("Created");
		table.column("Labels");
		table.column("Size");
		table.column("Virtual Size");
		table.print(System.out);
		return null;
	}
}

