import org.apache.karaf.features.command.FeaturesCommandSupport;
import org.apache.karaf.features.command.*;


import java.util.HashSet;
import java.util.List;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.RequiredFeatureCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "uninstall", description = "Uninstalls a feature with the specified name and version.")
@Service
public class UninstallFeatureCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "features", description = "The name and version of the features to uninstall. A feature id looks like name/version. The version is optional.", required = true, multiValued = true)
    @Completion(RequiredFeatureCompleter.class)
    List<String> features;

    @Option(name = "-r", aliases = "--no-auto-refresh", description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    @Option(name = "-v", aliases = "--verbose", description = "Explain what is being done", required = false, multiValued = false)
    boolean verbose;

    @Option(name = "-t", aliases = "--simulate", description = "Perform a simulation only", required = false, multiValued = false)
    boolean simulate;

    @Option(name = "-g", aliases = "--region", description = "Region to install to")
    String region;

    protected void doExecute(FeaturesService admin) throws Exception {
        addOption(FeaturesService.Option.Simulate, simulate);
        addOption(FeaturesService.Option.Verbose, verbose);
        addOption(FeaturesService.Option.NoAutoRefreshBundles, noRefresh);
        admin.uninstallFeatures(new HashSet<>(features), region, options);
    }
}
