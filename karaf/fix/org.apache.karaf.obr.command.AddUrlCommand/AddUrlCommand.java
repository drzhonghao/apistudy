

import java.util.List;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "obr", name = "url-add", description = "Adds a list of repository URLs to the OBR service.")
@Service
public class AddUrlCommand {
	@Argument(index = 0, name = "urls", description = "Repository URLs to add to the OBR service separated by whitespaces", required = true, multiValued = true)
	List<String> urls;

	protected void doExecute(RepositoryAdmin admin) throws Exception {
		for (String url : urls) {
			admin.addRepository(url);
		}
	}
}

