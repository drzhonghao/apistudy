

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Service
@Command(scope = "booking", name = "list", description = "List bookings")
public class ListCommand implements Action {
	@Option(name = "--url", description = "Location of the SOAP service", required = false, multiValued = false)
	String url = "http://localhost:8181/cxf/example";

	@Override
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("ID");
		table.column("Customer");
		table.column("Flight");
		table.print(System.out);
		return null;
	}
}

