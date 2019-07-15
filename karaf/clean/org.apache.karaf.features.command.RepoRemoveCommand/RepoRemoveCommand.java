import org.apache.karaf.features.command.FeaturesCommandSupport;
import org.apache.karaf.features.command.*;


import java.net.URI;
import java.util.Set;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.InstalledRepoUriCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "repo-remove", description = "Removes the specified repository features service.")
@Service
public class RepoRemoveCommand extends FeaturesCommandSupport {

	@Argument(index = 0, name = "repository", description = "Shortcut name of the feature repository or the full URI", required = true, multiValued = false)
	@Completion(InstalledRepoUriCompleter.class)
	private String nameOrUrl;

	@Argument(index = 1, name = "Feature version", description = "The version of the feature if using the feature name. Should be empty if using the uri", required = false, multiValued = false)
	private String version;

    @Option(name = "-u", aliases = { "--uninstall-all" }, description = "Uninstall all features from the repository", required = false, multiValued = false)
    private boolean uninstall;

    protected void doExecute(FeaturesService featuresService) throws Exception {
    	URI uri;
		{
			Set<URI> uris = selectRepositories(nameOrUrl, version);
			if (uris.isEmpty()) {
				System.err.println("No matching repository for " + nameOrUrl + (version != null ? " / " + version : ""));
				return;
			}
			if (uris.size() > 1) {
				System.err.println("Multiple matching repositories for " + nameOrUrl + (version != null ? " / " + version : ""));
				return;
			}
			uri = uris.iterator().next();
		}

		System.out.println("Removing features repository: " + uri);
		featuresService.removeRepository(uri, uninstall);
    }
}
