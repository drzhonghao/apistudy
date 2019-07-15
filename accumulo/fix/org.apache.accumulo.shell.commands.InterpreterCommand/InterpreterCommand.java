

import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.util.interpret.ScanInterpreter;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.commands.ShellPluginConfigurationCommand;


public class InterpreterCommand extends ShellPluginConfigurationCommand {
	@Override
	public String description() {
		return "specifies a scan interpreter to interpret scan range and column arguments";
	}

	public static Class<? extends ScanInterpreter> getCurrentInterpreter(final String tableName, final Shell shellState) {
		return ShellPluginConfigurationCommand.getPluginClass(tableName, shellState, ScanInterpreter.class, Property.TABLE_INTERPRETER_CLASS);
	}
}

