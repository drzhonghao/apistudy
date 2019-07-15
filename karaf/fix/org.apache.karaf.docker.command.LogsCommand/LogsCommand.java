

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "logs", description = "Fetch the logs of a container")
@Service
public class LogsCommand {
	@Option(name = "--stdout", description = "Display stdout", required = false, multiValued = false)
	boolean stdout = true;

	@Option(name = "--stderr", description = "Display stderr", required = false, multiValued = false)
	boolean stderr;

	@Option(name = "--timestamps", description = "Show timestamps", required = false, multiValued = false)
	boolean timestamps;

	@Option(name = "--details", description = "Show extra details provided to logs", required = false, multiValued = false)
	boolean details;

	public Object execute() throws Exception {
		if ((!(stdout)) && (!(stderr))) {
			System.err.println("You have at least to choose one stream: stdout or stderr using the corresponding command options");
		}
		return null;
	}
}

