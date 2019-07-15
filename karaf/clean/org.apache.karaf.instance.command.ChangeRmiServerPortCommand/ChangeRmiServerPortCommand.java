import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.*;


import org.apache.karaf.instance.command.completers.InstanceCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "instance", name = "rmi-server-port-change", description = "Changes the RMI server port (used by management layer) of an existing instance.")
@Service
public class ChangeRmiServerPortCommand extends InstanceCommandSupport {

    @Argument(index = 0, name = "name", description = "The name of the container instance", required = true, multiValued = false)
    @Completion(InstanceCompleter.class)
    private String instance = null;

    @Argument(index = 1, name = "port", description = "The new RMI server port to set", required = true, multiValued = false)
    private int port = 0;

    protected Object doExecute() throws Exception {
        getExistingInstance(instance).changeRmiServerPort(port);
        return null;
    }

}
