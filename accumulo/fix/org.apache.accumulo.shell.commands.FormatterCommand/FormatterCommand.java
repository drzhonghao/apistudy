

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.util.format.Formatter;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.commands.ShellPluginConfigurationCommand;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public class FormatterCommand extends ShellPluginConfigurationCommand {
	private Option interpeterOption;

	@Override
	public String description() {
		return "specifies a formatter to use for displaying table entries";
	}

	public static Class<? extends Formatter> getCurrentFormatter(final String tableName, final Shell shellState) {
		return ShellPluginConfigurationCommand.getPluginClass(tableName, shellState, Formatter.class, Property.TABLE_FORMATTER_CLASS);
	}

	@Override
	public Options getOptions() {
		final Options options = super.getOptions();
		interpeterOption = new Option("i", "interpeter", false, "configure class as interpreter also");
		options.addOption(interpeterOption);
		return options;
	}

	@Override
	protected void setPlugin(final CommandLine cl, final Shell shellState, final String tableName, final String className) throws AccumuloException, AccumuloSecurityException {
		super.setPlugin(cl, shellState, tableName, className);
		if (cl.hasOption(interpeterOption.getOpt())) {
			shellState.getConnector().tableOperations().setProperty(tableName, Property.TABLE_INTERPRETER_CLASS.toString(), className);
		}
	}

	@Override
	protected void removePlugin(final CommandLine cl, final Shell shellState, final String tableName) throws AccumuloException, AccumuloSecurityException {
		super.removePlugin(cl, shellState, tableName);
		if (cl.hasOption(interpeterOption.getOpt())) {
			shellState.getConnector().tableOperations().removeProperty(tableName, Property.TABLE_INTERPRETER_CLASS.toString());
		}
	}
}

