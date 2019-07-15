

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "subsystem", name = "start", description = "Start the specified subsystems")
@Service
public class StartAction implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

