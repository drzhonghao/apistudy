

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "docker", name = "provision", description = "Create a Docker container using the current running Karaf instance")
@Service
public class ProvisionCommand {
	@Argument(index = 0, name = "name", description = "Name of the Docker container", required = true, multiValued = false)
	String name;

	@Option(name = "-c", aliases = "--copy", description = "Use directly the current Karaf instance working dir or make a copy", required = false, multiValued = false)
	boolean copy;

	@Option(name = "--sshPort", description = "Port number used by the Karaf SSH server", required = false, multiValued = false)
	String sshPort = "8101";

	@Option(name = "--jmxRmiPort", description = "Port number used by the Karaf JMX RMI MBeanServer", required = false, multiValued = false)
	String jmxRmiPort = "1099";

	@Option(name = "--jmxRmiRegistryPort", description = "Port number used by the Karaf JMX RMI Registry MBeanServer", required = false, multiValued = false)
	String jmxRmiRegistryPort = "44444";

	@Option(name = "--httpPort", description = "Port number used by the Karaf HTTP service", required = false, multiValued = false)
	String httpPort = "8181";

	public Object execute() throws Exception {
		return null;
	}
}

