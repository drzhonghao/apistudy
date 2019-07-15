

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jndi", name = "bind", description = "Bind an OSGi service in the JNDI context")
@Service
public class BindCommand implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

