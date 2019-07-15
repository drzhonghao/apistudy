

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "info", description = "Display system-wide information")
@Service
public class InfoCommand {
	public Object execute() throws Exception {
		return null;
	}
}

