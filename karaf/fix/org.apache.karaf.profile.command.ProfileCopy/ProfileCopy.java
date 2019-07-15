

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(name = "copy", scope = "profile", description = "Copies the specified source profile")
@Service
public class ProfileCopy implements Action {
	@Option(name = "-f", aliases = "--force", description = "Flag to allow overwriting the target profile (if exists).")
	private boolean force;

	@Argument(index = 1, required = true, name = "target profile", description = "Name of the target profile.")
	private String target;
}

