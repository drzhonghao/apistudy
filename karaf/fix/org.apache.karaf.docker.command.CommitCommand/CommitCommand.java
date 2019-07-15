

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "commit", description = "Create a new image from a container's changes")
@Service
public class CommitCommand {
	@Argument(index = 1, name = "repository", description = "Repository", required = true, multiValued = false)
	String repo;

	@Argument(index = 2, name = "tag", description = "Tag", required = true, multiValued = false)
	String tag;

	@Option(name = "--message", description = "Commit message", required = false, multiValued = false)
	String message = "";

	public Object execute() throws Exception {
		return null;
	}
}

