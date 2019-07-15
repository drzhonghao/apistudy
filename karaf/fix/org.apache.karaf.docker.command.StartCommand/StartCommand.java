

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "start", description = "Start one or more stopped containers")
@Service
public class StartCommand {
	public Object execute() throws Exception {
		return null;
	}
}

