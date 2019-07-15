

import java.util.List;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "obr", name = "start", description = "Deploys and starts a list of bundles using OBR.")
@Service
public class StartCommand {
	@Argument(index = 0, name = "bundles", description = "List of bundles to deploy (separated by whitespaces). The bundles are identified using the following syntax: symbolic_name,version where version is optional.", required = true, multiValued = true)
	protected List<String> bundles;

	@Option(name = "-d", aliases = { "--deployOptional" }, description = "Deploy optional bundles", required = false, multiValued = false)
	protected boolean deployOptional = false;

	protected void doExecute(RepositoryAdmin admin) throws Exception {
	}
}

