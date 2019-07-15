

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(name = "rename", scope = "profile", description = "Rename the specified source profile")
@Service
public class ProfileRename implements Action {
	@Option(name = "--version", description = "The profile version to rename. Defaults to the current default version.")
	private String versionId;

	@Option(name = "-f", aliases = "--force", description = "Flag to allow replacing the target profile (if exists).")
	private boolean force;

	@Argument(index = 1, required = true, name = "new profile name", description = "New name of the profile.")
	private String newName;
}

