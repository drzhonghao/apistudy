import org.apache.karaf.bundle.command.completers.*;


import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.List;

@Service
public class BundleSymbolicNameCompleter implements Completer {

    @Reference
    private BundleContext bundleContext;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        for (Bundle bundle : bundleContext.getBundles()) {
            delegate.getStrings().add(bundle.getSymbolicName());
        }
        return delegate.complete(session, commandLine, candidates);
    }

}
