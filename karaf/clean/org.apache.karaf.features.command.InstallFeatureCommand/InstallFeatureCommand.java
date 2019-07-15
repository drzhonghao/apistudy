import org.apache.karaf.features.command.FeaturesCommandSupport;
import org.apache.karaf.features.command.*;


import java.util.HashSet;
import java.util.List;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.AvailableFeatureCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "install", description = "Installs a feature with the specified name and version.")
@Service
public class InstallFeatureCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "features", description = "The name and version of the features to install. A feature id looks like name/version. The version is optional.", required = true, multiValued = true)
    @Completion(AvailableFeatureCompleter.class)
    List<String> features;

    @Option(name = "-r", aliases = "--no-auto-refresh", description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    @Option(name = "-s", aliases = "--no-auto-start", description = "Do not start the bundles", required = false, multiValued = false)
    boolean noStart;

    @Option(name = "-m", aliases = "--no-auto-manage", description = "Do not automatically manage bundles", required = false, multiValued = false)
    boolean noManage;

    @Option(name = "-v", aliases = "--verbose", description = "Explain what is being done", required = false, multiValued = false)
    boolean verbose;

    @Option(name = "-t", aliases = "--simulate", description = "Perform a simulation only", required = false, multiValued = false)
    boolean simulate;

    @Option(name = "-u", aliases = "--upgrade", description = "Perform an upgrade of feature if previous version are installed or install it", required = false, multiValued = false)
    boolean upgrade;

    @Option(name = "--store", description = "Store the resolution into the given file and result for offline analysis")
    String outputFile;

    @Option(name = "--features-wiring", description = "Print the wiring between features")
    boolean featuresWiring;

    @Option(name = "--all-wiring", description = "Print the full wiring")
    boolean allWiring;

    @Option(name = "-g", aliases = "--region", description = "Region to install to")
    String region;

    protected void doExecute(FeaturesService admin) throws Exception {
        addOption(FeaturesService.Option.Simulate, simulate);
        addOption(FeaturesService.Option.NoAutoStartBundles, noStart);
        addOption(FeaturesService.Option.NoAutoRefreshBundles, noRefresh);
        addOption(FeaturesService.Option.NoAutoManageBundles, noManage);
        addOption(FeaturesService.Option.Verbose, verbose);
        addOption(FeaturesService.Option.Upgrade, upgrade);
        addOption(FeaturesService.Option.DisplayFeaturesWiring, featuresWiring);
        addOption(FeaturesService.Option.DisplayAllWiring, allWiring);
        admin.setResolutionOutputFile(outputFile);
        admin.installFeatures(new HashSet<>(features), region, options);
    }
}
