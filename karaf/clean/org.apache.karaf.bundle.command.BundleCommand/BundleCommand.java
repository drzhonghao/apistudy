import org.apache.karaf.bundle.command.*;


import org.apache.karaf.bundle.command.completers.BundleSymbolicNameCompleter;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Unique bundle command.
 */
public abstract class BundleCommand implements Action {

    @Option(name = "--context", description = "Use the given bundle context")
    String context = "0";

    @Argument(index = 0, name = "id", description = "The bundle ID or name or name/version", required = true, multiValued = false)
    @Completion(BundleSymbolicNameCompleter.class)
    String id;

    @Reference
    BundleService bundleService;

    @Reference
    BundleContext bundleContext;

    public Object execute() throws Exception {
        Bundle bundle = bundleService.getBundle(id);
        return doExecute(bundle);
    }

    protected abstract Object doExecute(Bundle bundle) throws Exception;

    public void setBundleService(BundleService bundleService) {
        this.bundleService = bundleService;
    }

}
