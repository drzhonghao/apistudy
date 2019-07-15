import org.apache.karaf.features.command.*;


import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "feature", name = "requirement-list", description = "List provisioning requirements.")
@Service
public class RequirementList implements Action {

    @Reference
    private FeaturesService featuresService;

    @Option(name = "--no-format", description = "Disable table rendered output")
    boolean noFormat;

    @Override
    public Object execute() throws Exception {
        Map<String, Set<String>> requirements = featuresService.listRequirements();

        ShellTable table = new ShellTable();
        table.column("Region");
        table.column("Requirement");
        table.emptyTableText("No requirements defined");

        for (Map.Entry<String, Set<String>> entry : requirements.entrySet()) {
            for (String requirement : entry.getValue()) {
                table.addRow().addContent(entry.getKey(), requirement);
            }
        }

        table.print(System.out, !noFormat);

        return null;
    }
}
