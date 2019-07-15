

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "rename", description = "Rename a container")
@Service
public class RenameCommand {
	@Argument(index = 1, name = "newName", description = "New name of the container", required = true, multiValued = false)
	String newName;

	public Object execute() throws Exception {
		return null;
	}
}

