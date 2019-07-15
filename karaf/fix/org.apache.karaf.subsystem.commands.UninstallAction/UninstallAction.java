

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "subsystem", name = "uninstall", description = "Uninstall the specified subsystems")
@Service
public class UninstallAction implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

