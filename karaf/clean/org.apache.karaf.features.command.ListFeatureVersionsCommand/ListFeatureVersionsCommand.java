import org.apache.karaf.features.command.*;


import java.util.Arrays;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.command.completers.AllFeatureCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "feature", name = "version-list", description = "Lists all versions of a feature available from the currently available repositories.")
@Service
public class ListFeatureVersionsCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "feature", description = "Name of feature.", required = true, multiValued = false)
    @Completion(AllFeatureCompleter.class)
	String feature;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    protected void doExecute(FeaturesService admin) throws Exception {
        ShellTable table = new ShellTable();
        table.column("Version");
        table.column("Repository");
        table.column("Repository URL");
        table.emptyTableText("No versions available for features '" + feature + "'");
             
        for (Repository r : Arrays.asList(admin.listRepositories())) {
            for (Feature f : r.getFeatures()) {

                if (f.getName().equals(feature)) {
                    table.addRow().addContent(f.getVersion(), r.getName(), r.getURI());
                }
            }
        }

        table.print(System.out, !noFormat);
    }

}
