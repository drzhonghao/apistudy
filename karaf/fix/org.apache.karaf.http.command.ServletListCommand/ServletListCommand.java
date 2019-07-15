

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "http", name = "list", description = "Lists details for servlets.")
@Service
public class ServletListCommand implements Action {
	@Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
	boolean noFormat;

	@Override
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column(new Col("ID"));
		table.column(new Col("Servlet"));
		table.column(new Col("Servlet-Name"));
		table.column(new Col("State"));
		table.column(new Col("Alias"));
		table.column(new Col("Url"));
		table.print(System.out, (!(noFormat)));
		return null;
	}
}

