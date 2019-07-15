import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.*;


import org.apache.karaf.instance.command.completers.InstanceCompleter;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "instance", name = "status", description = "Check the current status of an instance.")
@Service
public class StatusCommand extends InstanceCommandSupport {

    @Argument(index = 0, name = "name", description = "The name of the instance", required = true, multiValued = false)
    @Completion(InstanceCompleter.class)
    private String name;

    protected Object doExecute() throws Exception {
        Instance instance = getExistingInstance(name);
        System.out.println(instance.getState());
        return null;
    }

}
