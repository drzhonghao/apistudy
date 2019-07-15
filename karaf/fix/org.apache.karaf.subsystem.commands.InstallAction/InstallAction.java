

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "subsystem", name = "install", description = "Install a new subsystem")
@Service
public class InstallAction implements Action {
	@Argument(name = "New subsystem url", index = 1)
	String location;

	@Override
	public Object execute() throws Exception {
		return null;
	}
}

