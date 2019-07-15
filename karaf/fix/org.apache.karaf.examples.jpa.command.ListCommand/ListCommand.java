

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Service
@Command(scope = "booking", name = "list", description = "List the current bookings")
public class ListCommand implements Action {
	@Override
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("ID");
		table.column("Flight");
		table.column("Customer");
		table.print(System.out);
		return null;
	}
}

