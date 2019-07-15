import org.apache.karaf.bundle.command.*;


import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.util.jaas.JaasHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;

@Command(scope = "bundle", name = "start-level", description = "Gets or sets the start level of a bundle.")
@Service
public class StartLevel extends BundleCommand {

    @Argument(index = 1, name = "startLevel", description = "The bundle's new start level", required = false, multiValued = false)
    Integer level;

    @Reference
    BundleService bundleService;

    @Reference
    Session session;

    protected Object doExecute(Bundle bundle) throws Exception {
        // Get package instance service.
        BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
        if (level == null) {
            System.out.println("Level " + bsl.getStartLevel());
        }
        else {
            int sbsl = bundleService.getSystemBundleThreshold();
            if ((level < sbsl) && (bsl.getStartLevel() >= sbsl)) {
                if (!JaasHelper.currentUserHasRole(BundleService.SYSTEM_BUNDLES_ROLE)) {
                    throw new IllegalArgumentException("Insufficient priviledges");
                }

            }
            bsl.setStartLevel(level);
        }
        return null;
    }

}
