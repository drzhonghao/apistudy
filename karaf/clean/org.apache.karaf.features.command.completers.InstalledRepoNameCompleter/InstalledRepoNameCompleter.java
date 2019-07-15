import org.apache.karaf.features.command.completers.*;


import java.util.List;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * {@link Completer} for Feature Repository URLs.
 *
 * Displays a list of currently installed Feature repositories.
 *
 */
@Service
public class InstalledRepoNameCompleter implements Completer {

    @Reference
    private FeaturesService featuresService;

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public int complete(Session session, final CommandLine commandLine, final List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            for (Repository repository : featuresService.listRepositories()) {
                delegate.getStrings().add(repository.getName());
            }
        } catch (Exception e) {
            // Ignore
        }
        return delegate.complete(session, commandLine, candidates);
    }

}
