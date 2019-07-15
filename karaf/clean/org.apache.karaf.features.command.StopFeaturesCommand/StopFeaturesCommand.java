import org.apache.karaf.features.command.FeaturesCommandSupport;
import org.apache.karaf.features.command.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.StartedFeatureCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "stop", description = "Stop features with the specified name and version.")
@Service
public class StopFeaturesCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "feature", description = "The name and version of the features to stop. A feature id looks like name/version.", required = true, multiValued = true)
    @Completion(StartedFeatureCompleter.class)
    List<String> features;

    @Option(name = "-v", aliases = "--verbose", description = "Explain what is being done", required = false, multiValued = false)
    boolean verbose;

    @Option(name = "-t", aliases = "--simulate", description = "Perform a simulation only", required = false, multiValued = false)
    boolean simulate;

    @Option(name = "-g", aliases = "--region", description = "Region to apply to")
    String region = FeaturesService.ROOT_REGION;
    
    protected void doExecute(FeaturesService admin) throws Exception {
        addOption(FeaturesService.Option.Simulate, simulate);
        addOption(FeaturesService.Option.Verbose, verbose);
        Map<String, Map<String, FeatureState>> stateChanges = new HashMap<>();
        Map<String, FeatureState> regionChanges = new HashMap<>();
        for (String featureId : getFeatureIds(admin, features)) {
            regionChanges.put(featureId, FeatureState.Resolved);
        }
        stateChanges.put(region, regionChanges);
        admin.updateFeaturesState(stateChanges, options);
    }

}
