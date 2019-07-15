import org.apache.karaf.features.command.*;


import java.net.URI;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.AvailableRepoNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "repo-add", description = "Add a features repository")
@Service
public class RepoAddCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "name/url", description = "Shortcut name of the features repository or the full URL", required = true, multiValued = false)
    @Completion(AvailableRepoNameCompleter.class)
    private String nameOrUrl;
    
    @Argument(index = 1, name = "version", description = "The version of the features repository if using features repository name as first argument. It should be empty if using the URL", required = false, multiValued = false)
    private String version;

    @Option(name = "-i", aliases = { "--install" }, description = "Install all features contained in the features repository", required = false, multiValued = false)
    private boolean install;

    @Override
    protected void doExecute(FeaturesService featuresService) throws Exception {
        URI uri = featuresService.getRepositoryUriFor(nameOrUrl, version);
        if (uri == null) {
            uri = new URI(nameOrUrl);
        }
        if (featuresService.isRepositoryUriBlacklisted(uri)) {
            System.out.println("Feature URL " + uri + " is blacklisted");
            return;
        }
        System.out.println("Adding feature url " + uri);
        featuresService.addRepository(uri, install);
    }

}
