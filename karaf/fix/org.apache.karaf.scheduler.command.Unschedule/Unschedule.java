

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "scheduler", name = "unschedule", description = "Unschedule a job")
@Service
public class Unschedule implements Action {
	@Override
	public Object execute() throws Exception {
		return null;
	}
}

