

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "scheduler", name = "trigger", description = "Manually trigger a scheduled job")
@Service
public class Trigger implements Action {
	@Argument(description = "Name of the job to trigger", required = true)
	String name;

	@Option(name = "-b", aliases = "background", description = "schedule the trigger in the background", required = false)
	boolean background = false;
}

