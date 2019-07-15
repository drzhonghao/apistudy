

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "unpause", description = "Unpause all processes within one or more containers")
@Service
public class UnpauseCommand {
	public Object execute() throws Exception {
		return null;
	}
}

