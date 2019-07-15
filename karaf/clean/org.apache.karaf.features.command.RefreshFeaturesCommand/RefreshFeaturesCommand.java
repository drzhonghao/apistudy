import org.apache.karaf.features.command.FeaturesCommandSupport;
import org.apache.karaf.features.command.*;


import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "refresh", description = "Reloads features processing instructions and reprovisions existing features.")
@Service
public class RefreshFeaturesCommand extends FeaturesCommandSupport {

    @Option(name = "-v", aliases = "--verbose", description = "Explain what is being done", required = false, multiValued = false)
    boolean verbose;

    @Option(name = "-t", aliases = "--simulate", description = "Perform a simulation only", required = false, multiValued = false)
    boolean simulate;

    @Option(name = "--features-wiring", description = "Print the wiring between features")
    boolean featuresWiring;

    @Option(name = "--all-wiring", description = "Print the full wiring")
    boolean allWiring;

    protected void doExecute(FeaturesService featuresService) throws Exception {
        addOption(FeaturesService.Option.Simulate, simulate);
        addOption(FeaturesService.Option.Verbose, verbose);
        addOption(FeaturesService.Option.DisplayFeaturesWiring, featuresWiring);
        addOption(FeaturesService.Option.DisplayAllWiring, allWiring);
        try {
            featuresService.refreshFeatures(options);
        } catch (Exception e) {
            System.err.println("Error refreshing features: " + e.getMessage());
        }
    }

}
