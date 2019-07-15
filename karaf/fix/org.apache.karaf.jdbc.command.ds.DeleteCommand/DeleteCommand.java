

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jdbc", name = "ds-delete", description = "Delete a JDBC datasource")
@Service
public class DeleteCommand {
	public Object execute() throws Exception {
		return null;
	}
}

