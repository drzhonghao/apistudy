

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "jdbc", name = "tables", description = "List the tables on a given JDBC datasource")
@Service
public class TablesCommand {
	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		int rowCount = 0;
		for (int i = 0; i < rowCount; i++) {
			Row row = table.addRow();
		}
		table.print(System.out);
		return null;
	}
}

