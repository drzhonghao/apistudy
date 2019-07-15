

import java.util.List;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(name = "create", scope = "profile", description = "Create a new profile with the specified name and parents", detailedDescription = "classpath:profileCreate.txt")
@Service
public class ProfileCreate implements Action {
	@Option(name = "--parents", multiValued = true, required = false, description = "Optionally specifies one or multiple parent profiles. To specify multiple parent profiles, specify this flag multiple times on the command line. For example, --parents foo --parents bar.")
	private List<String> parents;

	@Argument(index = 0)
	private String profileId;

	@Override
	public Object execute() throws Exception {
		return null;
	}
}

