import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.*;


import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "instance", name = "list", description = "Lists all existing container instances.")
@Service
public class ListCommand extends InstanceCommandSupport {

    @Option(name = "-l", aliases = { "--location" }, description = "Displays the location of the container instances", required = false, multiValued = false)
    boolean location;

    @Option(name = "-o", aliases = { "--java-opts" }, description = "Displays the Java options used to launch the JVM", required = false, multiValued = false)
    boolean javaOpts;

    @Option(name = "--no-color", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    protected Object doExecute() throws Exception {
        Instance[] instances = getInstanceService().getInstances();
        ShellTable table = new ShellTable();
        table.column("SSH Port").alignRight();
        table.column("SSH Host").alignRight();
        table.column("RMI Registry").alignRight();
        table.column("RMI Registry Host").alignRight();
        table.column("RMI Server").alignRight();
        table.column("RMI Server Host").alignRight();
        table.column("State");
        table.column("PID");
        table.column(getRightColumnHeader());
        for (Instance instance : instances) {
            table.addRow().addContent(
                    instance.getSshPort(),
                    instance.getSshHost(),
                    instance.getRmiRegistryPort(),
                    instance.getRmiRegistryHost(),
                    instance.getRmiServerPort(),
                    instance.getRmiServerHost(),
                    instance.getState(),
                    instance.getPid(),
                    getRightColumnValue(instance));
        }
        table.print(System.out, !noFormat);
        return null;
    }

    private String getRightColumnHeader() {
        if (javaOpts) {
            return "JavaOpts";
        } else if (location) {
            return "Location";
        } else {
            return "Name";
        }
    }

    private String getRightColumnValue(Instance instance) {
        if (javaOpts) {
            return instance.getJavaOpts();
        } else if (location) {
            return instance.getLocation();
        } else {
            return instance.getName();
        }
    }

}
