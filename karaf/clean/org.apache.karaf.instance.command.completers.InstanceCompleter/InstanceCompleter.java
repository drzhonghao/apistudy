import org.apache.karaf.instance.command.completers.*;


import java.util.List;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * Displays a list of configured server instances for the instance commands.
 *
 */
@Service
public class InstanceCompleter implements Completer {

    @Reference
    private InstanceService instanceService;

    public void setInstanceService(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        for (Instance instance : instanceService.getInstances()) {
            if (acceptsInstance(instance)) {
                delegate.getStrings().add(instance.getName());
            }
        }
        return delegate.complete(session, commandLine, candidates);
    }

    protected boolean acceptsInstance(Instance instance) {
        return true;
    }
}
