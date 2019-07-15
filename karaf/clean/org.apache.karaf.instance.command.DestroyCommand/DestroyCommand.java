import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.*;


import java.util.List;

import org.apache.karaf.instance.command.completers.InstanceCompleter;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.MultiException;

/**
 * Destroy an existing instance.
 */
@Command(scope = "instance", name = "destroy", description = "Destroys an existing container instance.")
@Service
public class DestroyCommand extends InstanceCommandSupport
{
    @Argument(index = 0, name = "name", description= "The name of the container instance to destroy", required = true, multiValued = true)
    @Completion(InstanceCompleter.class)
    private List<String> instances = null;

    @SuppressWarnings("deprecation")
    protected Object doExecute() throws Exception {
        final MultiException exception = new MultiException("Error destroying instance(s)");
        for (Instance instance : getMatchingInstances(instances)) {
            try {
                instance.destroy();
            } catch (Exception e) {
                exception.addException(e);
            }
        }
        exception.throwIfExceptions();
        return null;
    }

}
