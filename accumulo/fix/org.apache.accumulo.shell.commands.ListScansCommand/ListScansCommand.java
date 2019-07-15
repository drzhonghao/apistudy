

import java.util.ArrayList;
import java.util.List;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.shell.Shell;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public class ListScansCommand extends Shell.Command {
	private Option tserverOption;

	private Option disablePaginationOpt;

	@Override
	public String description() {
		return "lists what scans are currently running in accumulo. See the" + (" accumulo.core.client.admin.ActiveScan javadoc for more information" + " about columns.");
	}

	@Override
	public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) throws Exception {
		List<String> tservers;
		final InstanceOperations instanceOps = shellState.getConnector().instanceOperations();
		final boolean paginate = !(cl.hasOption(disablePaginationOpt.getOpt()));
		if (cl.hasOption(tserverOption.getOpt())) {
			tservers = new ArrayList<>();
			tservers.add(cl.getOptionValue(tserverOption.getOpt()));
		}else {
			tservers = instanceOps.getTabletServers();
		}
		return 0;
	}

	@Override
	public int numArgs() {
		return 0;
	}

	@Override
	public Options getOptions() {
		final Options opts = new Options();
		tserverOption = new Option("ts", "tabletServer", true, "tablet server to list scans for");
		tserverOption.setArgName("tablet server");
		opts.addOption(tserverOption);
		disablePaginationOpt = new Option("np", "no-pagination", false, "disable pagination of output");
		opts.addOption(disablePaginationOpt);
		return opts;
	}
}

