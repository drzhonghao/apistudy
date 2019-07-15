

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


@Command(scope = "obr", name = "find", description = "Find OBR bundles for a given filter.")
@Service
public class FindCommand {
	@Argument(index = 0, name = "requirements", description = "Requirement", required = true, multiValued = true)
	List<String> requirements;

	protected void doExecute(RepositoryAdmin admin) throws Exception {
	}

	private void printResource(PrintStream out, Resource resource) {
		String name = resource.getPresentationName();
		if (name == null) {
			name = resource.getSymbolicName();
		}
		out.println(name);
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
			boolean hdr = false;
			for (Requirement req : reqs) {
				if (!(req.isOptional())) {
					if (!hdr) {
						hdr = true;
						out.println("Requirements:");
					}
					out.println(((("   " + (req.getName())) + ":") + (req.getFilter())));
				}
			}
			hdr = false;
			for (Requirement req : reqs) {
				if (req.isOptional()) {
					if (!hdr) {
						hdr = true;
						out.println("Optional Requirements:");
					}
					out.println(((("   " + (req.getName())) + ":") + (req.getFilter())));
				}
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

