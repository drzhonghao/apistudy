

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.service.command.ListServices;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.framework.BundleContext;


@Service
public class ObjectClassCompleter implements Completer {
	@Reference
	private BundleContext context;

	public void setContext(BundleContext context) {
		this.context = context;
	}

	@Override
	public int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
		Map<String, Integer> serviceNamesMap = ListServices.getServiceNamesMap(context);
		Set<String> serviceNames = serviceNamesMap.keySet();
		List<String> strings = new ArrayList<>();
		for (String name : serviceNames) {
		}
		strings.addAll(serviceNames);
		return new StringsCompleter(strings).complete(session, commandLine, candidates);
	}
}

