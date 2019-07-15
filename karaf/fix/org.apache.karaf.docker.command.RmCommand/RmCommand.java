

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "rm", description = "Remove one or more containers")
@Service
public class RmCommand {
	@Option(name = "--removeVolumes", description = "Remove the container volumes", required = false, multiValued = true)
	boolean removeVolumes;

	@Option(name = "--force", description = "Force remove container", required = false, multiValued = true)
	boolean force;

	public Object execute() throws Exception {
		return null;
	}
}

