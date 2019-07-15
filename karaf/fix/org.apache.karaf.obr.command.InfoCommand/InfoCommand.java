

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "obr", name = "info", description = "Prints information about OBR bundles.")
@Service
public class InfoCommand {
	@Argument(index = 0, name = "bundles", description = "Specify bundles to query for information (separated by whitespaces). The bundles are identified using the following syntax: symbolic_name,version where version is optional.", required = true, multiValued = true)
	List<String> bundles;

	protected void doExecute(RepositoryAdmin admin) throws Exception {
		for (String bundle : bundles) {
		}
	}

	private void printResource(PrintStream out, Resource resource) {
		if ((out != null) && (resource != null)) {
			Map map = resource.getProperties();
			for (Object o : map.entrySet()) {
				Map.Entry entry = ((Map.Entry) (o));
				if (entry.getValue().getClass().isArray()) {
					out.println(((entry.getKey()) + ":"));
					for (int j = 0; j < (Array.getLength(entry.getValue())); j++) {
						out.println(("   " + (Array.get(entry.getValue(), j))));
					}
				}else {
					out.println((((entry.getKey()) + ": ") + (entry.getValue())));
				}
			}
			Requirement[] reqs = resource.getRequirements();
			if ((reqs != null) && ((reqs.length) > 0)) {
				out.println("Requires:");
				for (Requirement req : reqs) {
					out.println(((("   " + (req.getName())) + ":") + (req.getFilter())));
				}
			}
			Capability[] caps = resource.getCapabilities();
			if ((caps != null) && ((caps.length) > 0)) {
				out.println("Capabilities:");
				for (Capability cap : caps) {
					out.println(((("   " + (cap.getName())) + ":") + (cap.getPropertiesAsMap())));
				}
			}
		}
	}
}

