import org.apache.karaf.bundle.command.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.MultiException;
import org.osgi.framework.Bundle;

@Command(scope = "bundle", name = "restart", description = "Restarts bundles.")
@Service
public class Restart extends BundlesCommand {
    
    public Restart() {
        defaultAllBundles = false;
        errorMessage = "Error restarting bundle";
    }

    protected Object doExecute(List<Bundle> bundles) throws Exception {
        if (bundles.isEmpty()) {
            System.err.println("No bundles specified.");
            return null;
        }
        List<Exception> exceptions = new ArrayList<>();
        for (Bundle bundle : bundles) {
            try {
                bundle.stop(Bundle.STOP_TRANSIENT);
            } catch (Exception e) {
                exceptions.add(new Exception("Unable to stop bundle " + bundle.getBundleId() + ": " + e.getMessage(), e));
            }
        }
        for (Bundle bundle : bundles) {
            try {
                bundle.start(Bundle.START_TRANSIENT);
            } catch (Exception e) {
                exceptions.add(new Exception("Unable to start bundle " + bundle.getBundleId() + ": " + e.getMessage(), e));
            }
        }
        MultiException.throwIf("Error restarting bundles", exceptions);
        return null;
    }

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
    }

}
