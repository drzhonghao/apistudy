

import java.util.List;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "obr", name = "deploy", description = "Deploys a list of bundles using OBR service.")
@Service
public class DeployCommand {
	@Argument(index = 0, name = "bundles", description = "List of bundle names to deploy (separated by whitespaces). The bundles are identified using the following syntax: symbolic_name,version where version is optional.", required = true, multiValued = true)
	protected List<String> bundles;

	@Option(name = "-s", aliases = { "--start" }, description = "Start the deployed bundles", required = false, multiValued = false)
	protected boolean start = false;

	@Option(name = "-d", aliases = { "--deployOptional" }, description = "Deploy optional bundles", required = false, multiValued = false)
	protected boolean deployOptional = false;

	protected void doExecute(RepositoryAdmin admin) throws Exception {
	}
}

