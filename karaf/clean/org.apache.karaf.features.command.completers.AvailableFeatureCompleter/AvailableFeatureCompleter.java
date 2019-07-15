import org.apache.karaf.features.command.completers.*;


import org.apache.karaf.features.Feature;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 * {@link org.apache.karaf.shell.api.console.Completer} for features not installed yet.
 */
@Service
public class AvailableFeatureCompleter extends FeatureCompleterSupport {

    @Override
    protected boolean acceptsFeature(Feature feature) {
        return !featuresService.isInstalled(feature) && !feature.isHidden() && !feature.isBlacklisted();
    }

}
