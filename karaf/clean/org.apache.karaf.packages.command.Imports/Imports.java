import org.apache.karaf.imports.command.*;


import java.util.List;

import org.apache.karaf.packages.core.PackageRequirement;
import org.apache.karaf.packages.core.PackageService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.Bundle;

@Command(scope = "package", name = "imports", description = "Lists imported packages and the bundles that import them")
@Service
public class Imports implements Action {
    
    @Option(name = "--filter", description = "Only show package instead of full filter", required = false, multiValued = false)
    boolean showFilter;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;
    
    @Option(name = "--show-name-only", description = "Show only package name", required = false, multiValued = false)
    boolean showOnlyName = false;
    
    @Option(name = "-p", description = "Only show package starting with given name", required = false, multiValued = false)
    String packageName;
    
    @Option(name = "-b", description = "Only show imports of the given bundle id", required = false, multiValued = false)
    Integer bundleId;

    @Reference
    private PackageService packageService;

    @Override
    public Object execute() throws Exception {
        List<PackageRequirement> imports = packageService.getImports();
        ShellTable table = new ShellTable();
        if (showFilter) {
            table.column("Filter");
            table.column("Optional");
            table.column("ID");
            table.column("Bundle Name");
        } else {
            table.column("Package");
            if (!showOnlyName) {
                table.column("Version");
                table.column("Optional");
                table.column("ID");
                table.column("Bundle Name");
            }
        }

        for (PackageRequirement req : imports) {
            if (matchesFilter(req)) {
                Bundle bundle = req.getBundle();
                Row row = table.addRow();
                if (showFilter) {
                    row.addContent(req.getFilter());
                    row.addContent(getOptional(req),
                            bundle.getBundleId(),
                            bundle.getSymbolicName());
                } else {
                    row.addContent(req.getPackageName());
                    if (!showOnlyName) {
                        row.addContent(req.getVersionRange());
                        row.addContent(getOptional(req),
                                bundle.getBundleId(),
                                bundle.getSymbolicName());
                    }
                }
            }
        }
        table.print(System.out, !noFormat);
        return null;
    }

    private boolean matchesFilter(PackageRequirement req) {
        return (packageName == null || req.getPackageName().startsWith(packageName))
            && (bundleId == null || req.getBundle().getBundleId() == bundleId);
    }

    private String getOptional(PackageRequirement req) {
        if (!req.isOptional()) {
            return "";
        }
        return (req.isResolveable() ? "resolved" : "unresolved");
    }

}
