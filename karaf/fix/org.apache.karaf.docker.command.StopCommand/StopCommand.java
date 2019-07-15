

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "stop", description = "Stop one or more running containers")
@Service
public class StopCommand {
	@Option(name = "-t", aliases = "--time", description = "Seconds to wait for stop before killing it (default 10)", required = false, multiValued = false)
	int timeToWait = 10;

	public Object execute() throws Exception {
		return null;
	}
}

