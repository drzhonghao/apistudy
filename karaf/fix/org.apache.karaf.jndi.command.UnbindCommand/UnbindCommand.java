

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jndi", name = "unbind", description = "Unbind a JNDI name.")
@Service
public class UnbindCommand implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

