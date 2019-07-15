

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "scheduler", name = "reschedule", description = "Update scheduling of an existing job")
@Service
public class Reschedule implements Action {
	@Option(name = "--concurrent", description = "Should jobs run concurrently or not (defaults to false)")
	boolean concurrent;

	@Option(name = "--cron", description = "The cron expression")
	String cron;

	@Option(name = "--at", description = "Absolute date in ISO format (ex: 2014-05-13T13:56:45)")
	String at;

	@Option(name = "--times", description = "Number of times this job should be executed")
	int times = -1;

	@Option(name = "--period", description = "Time during executions (in seconds)")
	long period;
}

