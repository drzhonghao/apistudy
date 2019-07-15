import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.*;


import org.apache.karaf.instance.command.completers.InstanceCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "instance", name = "rename", description = "Rename an existing container instance.")
@Service
public class RenameCommand extends InstanceCommandSupport {

    @Option(name = "-v", aliases = {"--verbose"}, description = "Display actions performed by the command (disabled by default)", required = false, multiValued = false)
    boolean verbose = false;

    @Argument(index = 0, name = "name", description = "The name of the container instance to rename", required = true, multiValued = false)
    @Completion(InstanceCompleter.class)
    String instance = null;

    @Argument(index = 1, name = "new-name", description = "The new name of the container instance", required = true, multiValued = false)
    String newName = null;

    protected Object doExecute() throws Exception {
        getInstanceService().renameInstance(instance, newName, verbose);
        return null;
    }

}
