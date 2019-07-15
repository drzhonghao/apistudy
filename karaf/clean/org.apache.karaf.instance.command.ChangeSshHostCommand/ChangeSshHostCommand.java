import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.*;


import org.apache.karaf.instance.command.completers.StoppedInstanceCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "instance", name = "ssh-host-change", description = "Changes the secure shell host of an existing container instance.")
@Service
public class ChangeSshHostCommand extends InstanceCommandSupport {

    @Argument(index = 0, name = "name", description="The name of the container instance", required = true, multiValued = false)
    @Completion(StoppedInstanceCompleter.class)
    private String instance = null;

    @Argument(index = 1, name = "host", description = "The new secure shell host to set", required = true, multiValued = false)
    private String host = "0.0.0.0";

    protected Object doExecute() throws Exception {
        getExistingInstance(instance).changeSshHost(host);
        return null;
    }
}
