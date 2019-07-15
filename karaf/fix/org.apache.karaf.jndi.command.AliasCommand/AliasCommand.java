

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jndi", name = "alias", description = "Create a JNDI alias on a given name.")
@Service
public class AliasCommand implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

