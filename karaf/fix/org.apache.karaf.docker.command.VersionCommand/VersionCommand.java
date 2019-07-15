

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "version", description = "Show the Docker version information")
@Service
public class VersionCommand {
	public Object execute() throws Exception {
		return null;
	}
}

