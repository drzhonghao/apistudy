

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "jms", name = "connectionfactories", description = "List the JMS connection factories")
@Service
public class ConnectionFactoriesCommand {
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("JMS Connection Factory");
		table.print(System.out);
		return null;
	}
}

