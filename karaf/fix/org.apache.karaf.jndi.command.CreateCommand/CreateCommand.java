

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jndi", name = "create", description = "Create a new JNDI sub-context.")
@Service
public class CreateCommand implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

