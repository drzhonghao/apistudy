

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(name = "list", scope = "profile", description = "Lists all profiles")
@Service
public class ProfileList implements Action {
	@Option(name = "--hidden", description = "Display hidden profiles")
	private boolean hidden;
}

