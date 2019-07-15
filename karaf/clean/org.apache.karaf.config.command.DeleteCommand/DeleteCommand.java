import org.apache.karaf.config.command.ConfigCommandSupport;
import org.apache.karaf.config.command.*;


import org.apache.karaf.config.command.completers.ConfigurationCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "config", name = "delete", description = "Delete a configuration.")
@Service
public class DeleteCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "pid", description = "PID of the configuration", required = true, multiValued = false)
    @Completion(ConfigurationCompleter.class)
    String pid;

    @Option(name = "--force", aliases = {}, description = "Force the edition of this config, even if another one was under edition", required = false, multiValued = false)
    boolean force;

    protected Object doExecute() throws Exception {
        String oldPid = (String) this.session.get(PROPERTY_CONFIG_PID);
        if (oldPid != null && oldPid.equals(pid) && !force) {
            System.err.println("This config is being edited.  Cancel / update first, or use the --force option");
            return null;
        }

        this.configRepository.delete(pid);
        if (oldPid != null && oldPid.equals(pid) && !force) {
            this.session.put(PROPERTY_CONFIG_PID, null);
            this.session.put(PROPERTY_CONFIG_PROPS, null);
        }
        return null;
    }

}
