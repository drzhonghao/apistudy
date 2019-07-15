import org.apache.karaf.jndi.command.completers.*;


import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.util.List;

/**
 * Completer on the OSGi services ID.
 */
@Service
public class ServicesIdCompleter implements Completer {

    @Reference
    private BundleContext bundleContext;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            ServiceReference[] references = bundle.getRegisteredServices();
            if (references != null) {
                for (ServiceReference reference : references) {
                    if (reference.getProperty(Constants.SERVICE_ID) != null) {
                        delegate.getStrings().add(reference.getProperty(Constants.SERVICE_ID).toString());
                    }
                }
            }
        }
        return delegate.complete(session, commandLine, candidates);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
