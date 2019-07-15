import org.apache.karaf.features.command.completers.*;


import java.util.Arrays;
import java.util.List;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * Shows the list of feature repos that can be installed with their short name
 */
@Service
public class AvailableRepoNameCompleter implements Completer {

    @Reference
    private FeaturesService featuresService;

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public int complete(Session session, CommandLine commandLine, final List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter(Arrays.asList(featuresService.getRepositoryNames()));
        return delegate.complete(session, commandLine, candidates);
    }

}
