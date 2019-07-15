

import java.util.Map;
import java.util.Set;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command.CompletionSet;
import org.apache.accumulo.shell.ShellOptions;
import org.apache.accumulo.shell.Token;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;


public class SetAuthsCommand extends Shell.Command {
	private Option userOpt;

	private Option scanOptAuths;

	private Option clearOptAuths;

	@Override
	public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) throws AccumuloException, AccumuloSecurityException {
		final String user = cl.getOptionValue(userOpt.getOpt(), shellState.getConnector().whoami());
		final String scanOpts = (cl.hasOption(clearOptAuths.getOpt())) ? null : cl.getOptionValue(scanOptAuths.getOpt());
		Shell.log.debug(("Changed record-level authorizations for user " + user));
		return 0;
	}

	@Override
	public String description() {
		return "sets the maximum scan authorizations for a user";
	}

	@Override
	public void registerCompletion(final Token root, final Map<Shell.Command.CompletionSet, Set<String>> completionSet) {
		registerCompletionForUsers(root, completionSet);
	}

	@Override
	public Options getOptions() {
		final Options o = new Options();
		final OptionGroup setOrClear = new OptionGroup();
		scanOptAuths = new Option("s", "scan-authorizations", true, "scan authorizations to set");
		scanOptAuths.setArgName("comma-separated-authorizations");
		setOrClear.addOption(scanOptAuths);
		clearOptAuths = new Option("c", "clear-authorizations", false, "clear the scan authorizations");
		setOrClear.addOption(clearOptAuths);
		setOrClear.setRequired(true);
		o.addOptionGroup(setOrClear);
		userOpt = new Option(ShellOptions.userOption, "user", true, "user to operate on");
		userOpt.setArgName("user");
		o.addOption(userOpt);
		return o;
	}

	@Override
	public int numArgs() {
		return 0;
	}
}

