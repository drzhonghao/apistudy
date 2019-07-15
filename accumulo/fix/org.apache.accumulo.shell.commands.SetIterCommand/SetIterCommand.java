

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jline.console.ConsoleReader;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.NamespaceOperations;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.AggregatingIterator;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.iterators.OptionDescriber;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.aggregation.Aggregator;
import org.apache.accumulo.core.iterators.user.AgeOffFilter;
import org.apache.accumulo.core.iterators.user.RegExFilter;
import org.apache.accumulo.core.iterators.user.ReqVisFilter;
import org.apache.accumulo.core.iterators.user.VersioningIterator;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.ShellCommandException;
import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.majc;
import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.minc;
import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.scan;
import static org.apache.accumulo.shell.ShellCommandException.ErrorCode.INITIALIZATION_FAILURE;


public class SetIterCommand extends Shell.Command {
	private Option allScopeOpt;

	private Option mincScopeOpt;

	private Option majcScopeOpt;

	private Option scanScopeOpt;

	Option profileOpt;

	Option priorityOpt;

	Option nameOpt;

	Option aggTypeOpt;

	Option ageoffTypeOpt;

	Option regexTypeOpt;

	Option versionTypeOpt;

	Option reqvisTypeOpt;

	Option classnameTypeOpt;

	@Override
	public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException, ShellCommandException {
		boolean tables = (cl.hasOption(OptUtil.tableOpt().getOpt())) || (!(shellState.getTableName().isEmpty()));
		boolean namespaces = cl.hasOption(OptUtil.namespaceOpt().getOpt());
		final int priority = Integer.parseInt(cl.getOptionValue(priorityOpt.getOpt()));
		final Map<String, String> options = new HashMap<>();
		String classname = cl.getOptionValue(classnameTypeOpt.getOpt());
		if (cl.hasOption(aggTypeOpt.getOpt())) {
			Shell.log.warn("aggregators are deprecated");
			@SuppressWarnings("deprecation")
			String deprecatedClassName = AggregatingIterator.class.getName();
			classname = deprecatedClassName;
		}else
			if (cl.hasOption(regexTypeOpt.getOpt())) {
				classname = RegExFilter.class.getName();
			}else
				if (cl.hasOption(ageoffTypeOpt.getOpt())) {
					classname = AgeOffFilter.class.getName();
				}else
					if (cl.hasOption(versionTypeOpt.getOpt())) {
						classname = VersioningIterator.class.getName();
					}else
						if (cl.hasOption(reqvisTypeOpt.getOpt())) {
							classname = ReqVisFilter.class.getName();
						}




		String currentTableName = null;
		String tmpTable = null;
		String configuredName;
		try {
			if (((profileOpt) != null) && (StringUtils.isBlank(shellState.getTableName()))) {
				currentTableName = shellState.getTableName();
				tmpTable = "accumulo.metadata";
				shellState.setTableName(tmpTable);
				tables = (cl.hasOption(OptUtil.tableOpt().getOpt())) || (!(shellState.getTableName().isEmpty()));
			}
			ClassLoader classloader = shellState.getClassLoader(cl, shellState);
			configuredName = SetIterCommand.setUpOptions(classloader, shellState.getReader(), classname, options);
		} finally {
			if (tmpTable != null) {
				shellState.setTableName(currentTableName);
			}
		}
		String name = cl.getOptionValue(nameOpt.getOpt(), null);
		if ((null == name) && (null == configuredName)) {
			throw new IllegalArgumentException("No provided or default name for iterator");
		}else
			if (null == name) {
				name = configuredName;
			}

		if (namespaces) {
			try {
				setNamespaceProperties(cl, shellState, priority, options, classname, name);
			} catch (NamespaceNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		}else
			if (tables) {
				setTableProperties(cl, shellState, priority, options, classname, name);
			}else {
				throw new IllegalArgumentException("No table or namespace specified");
			}

		return 0;
	}

	protected void setTableProperties(final CommandLine cl, final Shell shellState, final int priority, final Map<String, String> options, final String classname, final String name) throws AccumuloException, AccumuloSecurityException, TableNotFoundException, ShellCommandException {
		final String tableName = OptUtil.getTableOpt(cl, shellState);
		final String aggregatorClass = options.get("aggregatorClass");
		@SuppressWarnings("deprecation")
		String deprecatedAggregatorClassName = Aggregator.class.getName();
		if ((aggregatorClass != null) && (!(shellState.getConnector().tableOperations().testClassLoad(tableName, aggregatorClass, deprecatedAggregatorClassName)))) {
			throw new ShellCommandException(INITIALIZATION_FAILURE, ((("Servers are unable to load " + aggregatorClass) + " as type ") + deprecatedAggregatorClassName));
		}
		for (Iterator<Map.Entry<String, String>> i = options.entrySet().iterator(); i.hasNext();) {
			final Map.Entry<String, String> entry = i.next();
			if (((entry.getValue()) == null) || (entry.getValue().isEmpty())) {
				i.remove();
			}
		}
		final EnumSet<IteratorUtil.IteratorScope> scopes = EnumSet.noneOf(IteratorUtil.IteratorScope.class);
		if ((cl.hasOption(allScopeOpt.getOpt())) || (cl.hasOption(mincScopeOpt.getOpt()))) {
			scopes.add(minc);
		}
		if ((cl.hasOption(allScopeOpt.getOpt())) || (cl.hasOption(majcScopeOpt.getOpt()))) {
			scopes.add(majc);
		}
		if ((cl.hasOption(allScopeOpt.getOpt())) || (cl.hasOption(scanScopeOpt.getOpt()))) {
			scopes.add(scan);
		}
		if (scopes.isEmpty()) {
			throw new IllegalArgumentException("You must select at least one scope to configure");
		}
		final IteratorSetting setting = new IteratorSetting(priority, name, classname, options);
		shellState.getConnector().tableOperations().attachIterator(tableName, setting, scopes);
	}

	protected void setNamespaceProperties(final CommandLine cl, final Shell shellState, final int priority, final Map<String, String> options, final String classname, final String name) throws AccumuloException, AccumuloSecurityException, NamespaceNotFoundException, ShellCommandException {
		final String namespace = OptUtil.getNamespaceOpt(cl, shellState);
		if (!(shellState.getConnector().namespaceOperations().testClassLoad(namespace, classname, SortedKeyValueIterator.class.getName()))) {
			throw new ShellCommandException(INITIALIZATION_FAILURE, ((("Servers are unable to load " + classname) + " as type ") + (SortedKeyValueIterator.class.getName())));
		}
		final String aggregatorClass = options.get("aggregatorClass");
		@SuppressWarnings("deprecation")
		String deprecatedAggregatorClassName = Aggregator.class.getName();
		if ((aggregatorClass != null) && (!(shellState.getConnector().namespaceOperations().testClassLoad(namespace, aggregatorClass, deprecatedAggregatorClassName)))) {
			throw new ShellCommandException(INITIALIZATION_FAILURE, ((("Servers are unable to load " + aggregatorClass) + " as type ") + deprecatedAggregatorClassName));
		}
		for (Iterator<Map.Entry<String, String>> i = options.entrySet().iterator(); i.hasNext();) {
			final Map.Entry<String, String> entry = i.next();
			if (((entry.getValue()) == null) || (entry.getValue().isEmpty())) {
				i.remove();
			}
		}
		final EnumSet<IteratorUtil.IteratorScope> scopes = EnumSet.noneOf(IteratorUtil.IteratorScope.class);
		if ((cl.hasOption(allScopeOpt.getOpt())) || (cl.hasOption(mincScopeOpt.getOpt()))) {
			scopes.add(minc);
		}
		if ((cl.hasOption(allScopeOpt.getOpt())) || (cl.hasOption(majcScopeOpt.getOpt()))) {
			scopes.add(majc);
		}
		if ((cl.hasOption(allScopeOpt.getOpt())) || (cl.hasOption(scanScopeOpt.getOpt()))) {
			scopes.add(scan);
		}
		if (scopes.isEmpty()) {
			throw new IllegalArgumentException("You must select at least one scope to configure");
		}
		final IteratorSetting setting = new IteratorSetting(priority, name, classname, options);
		shellState.getConnector().namespaceOperations().attachIterator(namespace, setting, scopes);
	}

	private static String setUpOptions(ClassLoader classloader, final ConsoleReader reader, final String className, final Map<String, String> options) throws IOException, ShellCommandException {
		String input;
		@SuppressWarnings("rawtypes")
		SortedKeyValueIterator untypedInstance;
		@SuppressWarnings("rawtypes")
		Class<? extends SortedKeyValueIterator> clazz;
		try {
			clazz = classloader.loadClass(className).asSubclass(SortedKeyValueIterator.class);
			untypedInstance = clazz.newInstance();
		} catch (ClassNotFoundException e) {
			StringBuilder msg = new StringBuilder("Unable to load ").append(className);
			if ((className.indexOf('.')) < 0) {
				msg.append("; did you use a fully qualified package name?");
			}else {
				msg.append("; class not found.");
			}
			throw new ShellCommandException(INITIALIZATION_FAILURE, msg.toString());
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (ClassCastException e) {
			String msg = (className + " loaded successfully but does not implement SortedKeyValueIterator.") + " This class cannot be used with this command.";
			throw new ShellCommandException(INITIALIZATION_FAILURE, msg);
		}
		@SuppressWarnings("unchecked")
		SortedKeyValueIterator<Key, Value> skvi = untypedInstance;
		OptionDescriber iterOptions = null;
		if (OptionDescriber.class.isAssignableFrom(skvi.getClass())) {
			iterOptions = ((OptionDescriber) (skvi));
		}
		String iteratorName;
		if (null != iterOptions) {
			final OptionDescriber.IteratorOptions itopts = iterOptions.describeOptions();
			iteratorName = itopts.getName();
			if (iteratorName == null) {
				throw new IllegalArgumentException((className + " described its default distinguishing name as null"));
			}
			String shortClassName = className;
			if (className.contains(".")) {
				shortClassName = className.substring(((className.lastIndexOf('.')) + 1));
			}
			final Map<String, String> localOptions = new HashMap<>();
			do {
				for (String key : localOptions.keySet()) {
					options.remove(key);
				}
				localOptions.clear();
				reader.println(itopts.getDescription());
				String prompt;
				if ((itopts.getNamedOptions()) != null) {
					for (Map.Entry<String, String> e : itopts.getNamedOptions().entrySet()) {
						prompt = (((((((Shell.repeat("-", 10)) + "> set ") + shortClassName) + " parameter ") + (e.getKey())) + ", ") + (e.getValue())) + ": ";
						reader.flush();
						input = reader.readLine(prompt);
						if (input == null) {
							reader.println();
							throw new IOException("Input stream closed");
						}
						localOptions.put(e.getKey(), input);
					}
				}
				if ((itopts.getUnnamedOptionDescriptions()) != null) {
					for (String desc : itopts.getUnnamedOptionDescriptions()) {
						reader.println((((Shell.repeat("-", 10)) + "> entering options: ") + desc));
						input = "start";
						prompt = (((Shell.repeat("-", 10)) + "> set ") + shortClassName) + " option (<name> <value>, hit enter to skip): ";
						while (true) {
							reader.flush();
							input = reader.readLine(prompt);
							if (input == null) {
								reader.println();
								throw new IOException("Input stream closed");
							}else {
								input = new String(input);
							}
							if ((input.length()) == 0)
								break;

							String[] sa = input.split(" ", 2);
							localOptions.put(sa[0], sa[1]);
						} 
					}
				}
				options.putAll(localOptions);
				if (!(iterOptions.validateOptions(options)))
					reader.println(("invalid options for " + (clazz.getName())));

			} while (!(iterOptions.validateOptions(options)) );
		}else {
			reader.flush();
			reader.println(("The iterator class does not implement OptionDescriber." + (" Consider this for better iterator configuration using this setiter" + " command.")));
			iteratorName = reader.readLine("Name for iterator (enter to skip): ");
			if (null == iteratorName) {
				reader.println();
				throw new IOException("Input stream closed");
			}else
				if (StringUtils.isWhitespace(iteratorName)) {
					iteratorName = null;
				}

			reader.flush();
			reader.println("Optional, configure name-value options for iterator:");
			String prompt = (Shell.repeat("-", 10)) + "> set option (<name> <value>, hit enter to skip): ";
			final HashMap<String, String> localOptions = new HashMap<>();
			while (true) {
				reader.flush();
				input = reader.readLine(prompt);
				if (input == null) {
					reader.println();
					throw new IOException("Input stream closed");
				}else
					if (StringUtils.isWhitespace(input)) {
						break;
					}

				String[] sa = input.split(" ", 2);
				localOptions.put(sa[0], sa[1]);
			} 
			options.putAll(localOptions);
		}
		return iteratorName;
	}

	@Override
	public String description() {
		return "sets a table-specific or namespace-specific iterator";
	}

	protected void setBaseOptions(Options options) {
		setPriorityOptions(options);
		setNameOptions(options);
		setIteratorTypeOptions(options);
	}

	private void setNameOptions(Options options) {
		nameOpt = new Option("n", "name", true, "iterator to set");
		nameOpt.setArgName("itername");
		options.addOption(nameOpt);
	}

	private void setPriorityOptions(Options options) {
		priorityOpt = new Option("p", "priority", true, "the order in which the iterator is applied");
		priorityOpt.setArgName("pri");
		priorityOpt.setRequired(true);
		options.addOption(priorityOpt);
	}

	@Override
	public Options getOptions() {
		final Options o = new Options();
		setBaseOptions(o);
		setScopeOptions(o);
		setTableOptions(o);
		return o;
	}

	private void setScopeOptions(Options o) {
		allScopeOpt = new Option("all", "all-scopes", false, "applied at scan time, minor and major compactions");
		mincScopeOpt = new Option(minc.name(), "minor-compaction", false, "applied at minor compaction");
		majcScopeOpt = new Option(majc.name(), "major-compaction", false, "applied at major compaction");
		scanScopeOpt = new Option(scan.name(), "scan-time", false, "applied at scan time");
		o.addOption(allScopeOpt);
		o.addOption(mincScopeOpt);
		o.addOption(majcScopeOpt);
		o.addOption(scanScopeOpt);
	}

	private void setTableOptions(Options o) {
		final OptionGroup tableGroup = new OptionGroup();
		tableGroup.addOption(OptUtil.tableOpt("table to configure iterators on"));
		tableGroup.addOption(OptUtil.namespaceOpt("namespace to configure iterators on"));
		o.addOptionGroup(tableGroup);
	}

	private void setIteratorTypeOptions(Options o) {
		final OptionGroup typeGroup = new OptionGroup();
		classnameTypeOpt = new Option("class", "class-name", true, "a java class that implements SortedKeyValueIterator");
		classnameTypeOpt.setArgName("name");
		aggTypeOpt = new Option("agg", "aggregator", false, "an aggregating type");
		regexTypeOpt = new Option("regex", "regular-expression", false, "a regex matching iterator");
		versionTypeOpt = new Option("vers", "version", false, "a versioning iterator");
		reqvisTypeOpt = new Option("reqvis", "require-visibility", false, "an iterator that omits entries with empty visibilities");
		ageoffTypeOpt = new Option("ageoff", "ageoff", false, "an aging off iterator");
		typeGroup.addOption(classnameTypeOpt);
		typeGroup.addOption(aggTypeOpt);
		typeGroup.addOption(regexTypeOpt);
		typeGroup.addOption(versionTypeOpt);
		typeGroup.addOption(reqvisTypeOpt);
		typeGroup.addOption(ageoffTypeOpt);
		typeGroup.setRequired(true);
		o.addOptionGroup(typeGroup);
	}

	@Override
	public int numArgs() {
		return 0;
	}
}

