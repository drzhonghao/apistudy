

import java.io.PrintStream;
import java.util.List;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "obr", name = "url-remove", description = "Removes a list of repository URLs from the OBR service.")
@Service
public class RemoveUrlCommand {
	@Option(name = "-i", aliases = { "--index" }, description = "Use index to identify URL", required = false, multiValued = false)
	boolean useIndex;

	@Argument(index = 0, name = "ids", description = "Repository URLs (or indexes if you use -i) to remove from OBR service", required = true, multiValued = true)
	List<String> ids;

	protected void doExecute(RepositoryAdmin admin) throws Exception {
		for (String id : ids) {
			if (useIndex) {
				Repository[] repos = admin.listRepositories();
				int index = Integer.parseInt(id);
				if ((index >= 0) && (index < (repos.length))) {
					admin.removeRepository(repos[index].getURI());
				}else {
					System.err.println("Invalid index");
				}
			}else {
				admin.removeRepository(id);
			}
		}
	}
}

