

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SecurityOperations;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.ShellCommandException;
import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.SetIterCommand;
import org.apache.accumulo.shell.commands.SetShellIterCommand;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.majc;
import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.minc;
import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.scan;


public class SetScanIterCommand extends SetIterCommand {
	@Override
	public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException, ShellCommandException {
		Shell.log.warn(("Deprecated, use " + (new SetShellIterCommand().getName())));
		return super.execute(fullCommand, cl, shellState);
	}

	@Override
	protected void setTableProperties(final CommandLine cl, final Shell shellState, final int priority, final Map<String, String> options, final String classname, final String name) throws AccumuloException, AccumuloSecurityException, TableNotFoundException, ShellCommandException {
		final String tableName = OptUtil.getTableOpt(cl, shellState);
		for (Iterator<Map.Entry<String, String>> i = options.entrySet().iterator(); i.hasNext();) {
			final Map.Entry<String, String> entry = i.next();
			if (((entry.getValue()) == null) || (entry.getValue().isEmpty())) {
				i.remove();
			}
		}
		List<IteratorSetting> tableScanIterators = shellState.scanIteratorOptions.get(tableName);
		if (tableScanIterators == null) {
			tableScanIterators = new ArrayList<>();
			shellState.scanIteratorOptions.put(tableName, tableScanIterators);
		}
		final IteratorSetting setting = new IteratorSetting(priority, name, classname);
		setting.addOptions(options);
		final String user = shellState.getConnector().whoami();
		final Authorizations auths = shellState.getConnector().securityOperations().getUserAuthorizations(user);
		final Scanner scanner = shellState.getConnector().createScanner(tableName, auths);
		for (IteratorSetting s : tableScanIterators) {
			scanner.addScanIterator(s);
		}
		scanner.addScanIterator(setting);
		tableScanIterators.add(setting);
		Shell.log.debug(("Scan iterators :" + (shellState.scanIteratorOptions.get(tableName))));
	}

	@Override
	public String description() {
		return "sets a table-specific scan iterator for this shell session";
	}

	@Override
	public Options getOptions() {
		final HashSet<OptionGroup> groups = new HashSet<>();
		final Options parentOptions = super.getOptions();
		final Options modifiedOptions = new Options();
		for (Iterator<?> it = parentOptions.getOptions().iterator(); it.hasNext();) {
			Option o = ((Option) (it.next()));
			if (((!(majc.name().equals(o.getOpt()))) && (!(minc.name().equals(o.getOpt())))) && (!(scan.name().equals(o.getOpt())))) {
				modifiedOptions.addOption(o);
				OptionGroup group = parentOptions.getOptionGroup(o);
				if (group != null)
					groups.add(group);

			}
		}
		for (OptionGroup group : groups) {
			modifiedOptions.addOptionGroup(group);
		}
		return modifiedOptions;
	}
}

