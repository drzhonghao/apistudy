

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "pull", description = "Pull an image")
@Service
public class PullCommand {
	@Argument(index = 0, name = "image", description = "The Docker image to pull", multiValued = false, required = true)
	String image;

	@Option(name = "-t", aliases = "--tag", description = "Tag to use", multiValued = false, required = false)
	String tag = "latest";

	@Option(name = "-v", aliases = "--verbose", description = "Display pulling progress on console", multiValued = false, required = false)
	boolean verbose;

	public Object execute() throws Exception {
		return null;
	}
}

