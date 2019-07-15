

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "push", description = "Push an image or a repository to a registry")
@Service
public class PushCommand {
	@Option(name = "--tag", description = "Push tag", required = false, multiValued = false)
	String tag = "latest";

	@Option(name = "-v", aliases = "--verbose", description = "Display push progress on console", required = false, multiValued = false)
	boolean verbose;

	public Object execute() throws Exception {
		return null;
	}
}

