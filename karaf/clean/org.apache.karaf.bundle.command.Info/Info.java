import org.apache.karaf.bundle.command.*;


import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.framework.Bundle;

@Command(scope = "bundle", name = "info", description = "Displays detailed information of a given bundles.")
@Service
public class Info extends BundlesCommand {

    @Reference
    Session session;

    /**
     * <p>
     * Get the OSGI-INF/bundle.info entry from the bundle and display it.
     * </p>
     *
     * @param bundle the bundle.
     */
    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
        session.execute("*:help 'bundle|" + bundle.getBundleId() + "'");
    }

}
