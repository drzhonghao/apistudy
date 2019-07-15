import org.apache.karaf.features.command.*;


import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.net.URI;

@Command(scope = "feature", name = "provided", description = "List the features provided by a features repository")
@Service
public class ListProvidedFeaturesCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "repo", description = "The features repository URI", required = true, multiValued = false)
    URI featuresRepositoryUri;

    @Override
    protected void doExecute(FeaturesService service) throws Exception {
        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Version");
        for (Feature feature : service.repositoryProvidedFeatures(featuresRepositoryUri)) {
            table.addRow().addContent(feature.getName(), feature.getVersion());
        }
        table.print(System.out);
    }

}
