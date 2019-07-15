import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.*;


import org.apache.karaf.instance.command.completers.InstanceCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "instance", name = "opts-change", description = "Changes the Java options of an existing container instance.")
@Service
public class ChangeOptsCommand extends InstanceCommandSupport {

    @Argument(index = 0, name = "name", description="The name of the container instance", required = true, multiValued = false)
    @Completion(InstanceCompleter.class)
    private String instance = null;

    @Argument(index = 1, name = "javaOpts", description = "The new Java options to set", required = true, multiValued = false)
    private String javaOpts;

    protected Object doExecute() throws Exception {
        getExistingInstance(instance).changeJavaOpts(javaOpts);
        return null;
    }

}
