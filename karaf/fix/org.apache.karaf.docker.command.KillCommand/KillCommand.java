

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "kill", description = "Kill one or more running containers")
@Service
public class KillCommand {
	@Option(name = "--signal", description = "The signal to send to the processes", required = false, multiValued = false)
	String signal = "SIGKILL";

	public Object execute() throws Exception {
		return null;
	}
}

