

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "restart", description = "Restart one or more containers")
@Service
public class RestartCommand {
	@Option(name = "-t", aliases = "--time", description = "Seconds to wait for stop before killing it (default 10)", required = false, multiValued = true)
	int timeToWait = 10;

	public Object execute() throws Exception {
		return null;
	}
}

