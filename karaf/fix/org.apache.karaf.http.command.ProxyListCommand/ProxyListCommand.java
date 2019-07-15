

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "http", name = "proxies", description = "List the HTTP proxies")
@Service
public class ProxyListCommand implements Action {
	@Override
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("URL");
		table.column("ProxyTo");
		table.print(System.out);
		return null;
	}
}

