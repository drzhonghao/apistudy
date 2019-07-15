

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "tag", description = "Create a tag TARGET_IMAGE that refers to SOURCE_IMAGE")
@Service
public class TagCommand {
	@Argument(index = 1, name = "tag", description = "Tag", required = true, multiValued = false)
	String tag;

	@Argument(index = 2, name = "repo", description = "Repository where to tag", required = false, multiValued = false)
	String repo;

	public Object execute() throws Exception {
		return null;
	}
}

