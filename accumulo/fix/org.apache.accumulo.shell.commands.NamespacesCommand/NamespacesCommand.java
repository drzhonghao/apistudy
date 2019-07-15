

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.NamespaceOperations;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.shell.Shell;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public class NamespacesCommand extends Shell.Command {
	private Option disablePaginationOpt;

	private Option namespaceIdOption;

	private static final String DEFAULT_NAMESPACE_DISPLAY_NAME = "\"\"";

	@Override
	public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) throws IOException, AccumuloException, AccumuloSecurityException {
		Map<String, String> namespaces = new TreeMap<>(shellState.getConnector().namespaceOperations().namespaceIdMap());
		Iterator<String> it = Iterators.transform(namespaces.entrySet().iterator(), new Function<Map.Entry<String, String>, String>() {
			@Override
			public String apply(Map.Entry<String, String> entry) {
				String name = entry.getKey();
				if (Namespaces.DEFAULT_NAMESPACE.equals(name))
					name = NamespacesCommand.DEFAULT_NAMESPACE_DISPLAY_NAME;

				String id = entry.getValue();
				if (cl.hasOption(namespaceIdOption.getOpt())) {
				}else
					return name;

				return null;
			}
		});
		shellState.printLines(it, (!(cl.hasOption(disablePaginationOpt.getOpt()))));
		return 0;
	}

	@Override
	public String description() {
		return "displays a list of all existing namespaces";
	}

	@Override
	public Options getOptions() {
		final Options o = new Options();
		namespaceIdOption = new Option("l", "list-ids", false, "display internal namespace ids along with the name");
		o.addOption(namespaceIdOption);
		disablePaginationOpt = new Option("np", "no-pagination", false, "disable pagination of output");
		o.addOption(disablePaginationOpt);
		return o;
	}

	@Override
	public int numArgs() {
		return 0;
	}
}

