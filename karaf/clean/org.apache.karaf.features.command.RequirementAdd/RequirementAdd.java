import org.apache.karaf.features.command.FeaturesCommandSupport;
import org.apache.karaf.features.command.*;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "requirement-add", description = "Add provisioning requirements.")
@Service
public class RequirementAdd extends FeaturesCommandSupport {

    @Argument(required = true, multiValued = true)
    List<String> requirements;

    @Option(name = "-r", aliases = "--no-auto-refresh", description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    @Option(name = "-s", aliases = "--no-auto-start", description = "Do not start the bundles", required = false, multiValued = false)
    boolean noStart;

    @Option(name = "-m", aliases = "--no-auto-manage", description = "Do not automatically manage bundles", required = false, multiValued = false)
    boolean noManage;

    @Option(name = "-v", aliases = "--verbose", description = "Explain what is being done")
    boolean verbose;

    @Option(name = "-t", aliases = "--simulate", description = "Perform a simulation only")
    boolean simulate;

    @Option(name = "--store", description = "Store the resolution into the given file and result for offline analysis")
    String outputFile;

    @Option(name = "--features-wiring", description = "Print the wiring between features")
    boolean featuresWiring;

    @Option(name = "--all-wiring", description = "Print the full wiring")
    boolean allWiring;

    @Option(name = "-g", aliases = "--region", description = "Region to install to")
    String region = FeaturesService.ROOT_REGION;

    @Override
    protected void doExecute(FeaturesService featuresService) throws Exception {
        addOption(FeaturesService.Option.Simulate, simulate);
        addOption(FeaturesService.Option.NoAutoStartBundles, noStart);
        addOption(FeaturesService.Option.NoAutoRefreshBundles, noRefresh);
        addOption(FeaturesService.Option.NoAutoManageBundles, noManage);
        addOption(FeaturesService.Option.Verbose, verbose);
        addOption(FeaturesService.Option.DisplayFeaturesWiring, featuresWiring);
        addOption(FeaturesService.Option.DisplayAllWiring, allWiring);
        Map<String, Set<String>> reqs = new HashMap<>();
        reqs.put(region, new HashSet<>(requirements));
        featuresService.setResolutionOutputFile(outputFile);
        featuresService.addRequirements(reqs, options);
    }

}
