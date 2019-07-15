import org.apache.karaf.features.command.completers.*;


import java.util.SortedSet;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 * {@link org.apache.karaf.shell.api.console.Completer} for features not installed yet.
 */
@Service
public class ResolvedFeatureCompleter extends FeatureCompleterSupport {

    @Override
    protected boolean acceptsFeature(Feature feature) {
        return featuresService.getState(feature.getId()) == FeatureState.Resolved;
    }

    protected void add(SortedSet<String> candidates, Feature feature) {
        candidates.add(feature.getId());
    }

}
