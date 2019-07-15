

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(name = "delete", scope = "profile", description = "Delete the specified profile")
@Service
public class ProfileDelete implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

