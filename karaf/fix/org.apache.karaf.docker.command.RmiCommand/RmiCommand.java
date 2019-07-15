

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "rmi", description = "Remove an image")
@Service
public class RmiCommand {
	@Option(name = "-f", aliases = "--force", description = "Force image remove", required = false, multiValued = false)
	boolean force;

	@Option(name = "-np", aliases = "--noprune", description = "Don't prune image", required = false, multiValued = false)
	boolean noprune;

	public Object execute() throws Exception {
		return null;
	}
}

