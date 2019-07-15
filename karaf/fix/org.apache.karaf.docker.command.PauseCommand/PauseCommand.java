

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "pause", description = "Pause all processes within one or more containers")
@Service
public class PauseCommand {
	public Object execute() throws Exception {
		return null;
	}
}

