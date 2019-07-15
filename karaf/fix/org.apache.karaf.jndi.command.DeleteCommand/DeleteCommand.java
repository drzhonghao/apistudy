

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jndi", name = "delete", description = "Delete a JNDI sub-context.")
@Service
public class DeleteCommand implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

