

import java.util.Calendar;
import java.util.Date;
import javax.xml.bind.DatatypeConverter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;


@Command(scope = "scheduler", name = "schedule-command", description = "Schedule a command execution")
@Service
public class ScheduleCommand implements Action {
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

	@Argument(name = "command", required = true, description = "The command to schedule")
	String command;

	@Reference
	Session session;

	@Override
	public Object execute() throws Exception {
		if (((cron) != null) && ((((at) != null) || ((times) != (-1))) || ((period) != 0))) {
			throw new IllegalArgumentException("Both cron expression and explicit execution time can not be specified");
		}
		if ((cron) != null) {
		}else {
			Date date;
			if ((at) != null) {
				date = DatatypeConverter.parseDateTime(at).getTime();
			}else {
				date = new Date();
			}
			if ((period) > 0) {
			}else {
			}
		}
		if (concurrent) {
		}
		return null;
	}
}

