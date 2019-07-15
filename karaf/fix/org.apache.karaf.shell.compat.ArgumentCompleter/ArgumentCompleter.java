

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf.shell.commands.CompleterValues;
import org.apache.karaf.shell.commands.HelpOption;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ArgumentCompleter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentCompleter.class);

	public static final String ARGUMENTS_LIST = "ARGUMENTS_LIST";

	public static final String COMMANDS = ".commands";

	final String scope;

	final String name;

	final Completer commandCompleter;

	final Completer optionsCompleter;

	final List<Completer> argsCompleters;

	final Map<String, Completer> optionalCompleters;

	final CommandWithAction function;

	final Map<Option, Field> fields = new HashMap<>();

	final Map<String, Option> options = new HashMap<>();

	final Map<Integer, Field> arguments = new HashMap<>();

	boolean strict = true;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArgumentCompleter(CommandWithAction function, String scope, String name, boolean scoped) {
		this.function = function;
		this.scope = scope;
		this.name = name;
		String[] names = (scoped) ? new String[]{ name } : new String[]{ name, (scope + ":") + name };
		for (Class<?> type = function.getActionClass(); type != null; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
				Option option = field.getAnnotation(Option.class);
				if (option != null) {
					fields.put(option, field);
					options.put(option.name(), option);
					String[] aliases = option.aliases();
					if (aliases != null) {
						for (String alias : aliases) {
							options.put(alias, option);
						}
					}
				}
				Argument argument = field.getAnnotation(Argument.class);
				if (argument != null) {
					Integer key = argument.index();
					if (arguments.containsKey(key)) {
						ArgumentCompleter.LOGGER.warn(((((("Duplicate @Argument annotations on class " + (type.getName())) + " for index: ") + key) + " see: ") + field));
					}else {
						arguments.put(key, field);
					}
				}
			}
		}
		options.put(HelpOption.HELP.name(), HelpOption.HELP);
		List<Completer> argsCompleters = null;
		Map<String, Completer> optionalCompleters = null;
		if (function instanceof CompletableFunction) {
		}
		if (argsCompleters == null) {
			final Map<Integer, Object> values = getCompleterValues(function);
			argsCompleters = new ArrayList<>();
			boolean multi = false;
			for (int key = 0; key < (arguments.size()); key++) {
				Completer completer = null;
				Field field = arguments.get(key);
				if (field != null) {
					Argument argument = field.getAnnotation(Argument.class);
					multi = (argument != null) && (argument.multiValued());
					org.apache.karaf.shell.commands.Completer ann = field.getAnnotation(org.apache.karaf.shell.commands.Completer.class);
					if (ann != null) {
						Class clazz = ann.value();
						String[] value = ann.values();
						if (clazz != null) {
							if (((value.length) > 0) && (clazz == (StringsCompleter.class))) {
							}else {
								BundleContext context = FrameworkUtil.getBundle(function.getClass()).getBundleContext();
								completer = new ArgumentCompleter.ProxyServiceCompleter(context, clazz);
							}
						}
					}else
						if (values.containsKey(key)) {
							Object value = values.get(key);
							if (value instanceof String[]) {
							}else
								if (value instanceof Collection) {
								}else {
									ArgumentCompleter.LOGGER.warn((("Could not use value " + value) + " as set of completions!"));
								}

						}else {
							completer = getDefaultCompleter(field);
						}

				}
				if (completer == null) {
				}
				argsCompleters.add(completer);
			}
			if ((argsCompleters.isEmpty()) || (!multi)) {
			}
			optionalCompleters = new HashMap<>();
			for (Option option : fields.keySet()) {
				Completer completer = null;
				Field field = fields.get(option);
				if (field != null) {
					org.apache.karaf.shell.commands.Completer ann = field.getAnnotation(org.apache.karaf.shell.commands.Completer.class);
					if (ann != null) {
						Class clazz = ann.value();
						String[] value = ann.values();
						if (clazz != null) {
							if (((value.length) > 0) && (clazz == (StringsCompleter.class))) {
							}else {
								BundleContext context = FrameworkUtil.getBundle(function.getClass()).getBundleContext();
								completer = new ArgumentCompleter.ProxyServiceCompleter(context, clazz);
							}
						}
					}
				}
				if (completer == null) {
				}
				optionalCompleters.put(option.name(), completer);
				if ((option.aliases()) != null) {
					for (String alias : option.aliases()) {
						optionalCompleters.put(alias, completer);
					}
				}
			}
		}
		this.argsCompleters = argsCompleters;
		this.optionalCompleters = optionalCompleters;
		optionsCompleter = null;
		commandCompleter = null;
	}

	private Map<Integer, Object> getCompleterValues(CommandWithAction function) {
		final Map<Integer, Object> values = new HashMap<>();
		Action action = null;
		try {
			for (Class<?> type = function.getActionClass(); type != null; type = type.getSuperclass()) {
				for (Method method : type.getDeclaredMethods()) {
					CompleterValues completerMethod = method.getAnnotation(CompleterValues.class);
					if (completerMethod != null) {
						int index = completerMethod.index();
						Integer key = index;
						if ((index >= (arguments.size())) || (index < 0)) {
							ArgumentCompleter.LOGGER.warn(((((("Index out of range on @CompleterValues on class " + (type.getName())) + " for index: ") + key) + " see: ") + method));
						}else
							if (values.containsKey(key)) {
								ArgumentCompleter.LOGGER.warn(((((("Duplicate @CompleterMethod annotations on class " + (type.getName())) + " for index: ") + key) + " see: ") + method));
							}else {
								try {
									Object value;
									if (Modifier.isStatic(method.getModifiers())) {
										value = method.invoke(null);
									}else {
										if (action == null) {
											action = function.createNewAction();
										}
										value = method.invoke(action);
									}
									values.put(key, value);
								} catch (IllegalAccessException e) {
									ArgumentCompleter.LOGGER.warn(((("Could not invoke @CompleterMethod on " + function) + ". ") + e), e);
								} catch (InvocationTargetException e) {
									Throwable target = e.getTargetException();
									if (target == null) {
										target = e;
									}
									ArgumentCompleter.LOGGER.warn(((("Could not invoke @CompleterMethod on " + function) + ". ") + target), target);
								}
							}

					}
				}
			}
		} finally {
			if (action != null) {
				try {
					function.releaseAction(action);
				} catch (Exception e) {
					ArgumentCompleter.LOGGER.warn(((("Failed to release action: " + action) + ". ") + e), e);
				}
			}
		}
		return values;
	}

	private Completer getDefaultCompleter(Field field) {
		Completer completer = null;
		Class<?> type = field.getType();
		if (type.isAssignableFrom(File.class)) {
		}else
			if ((type.isAssignableFrom(Boolean.class)) || (type.isAssignableFrom(boolean.class))) {
			}else
				if (type.isAssignableFrom(Enum.class)) {
					Set<String> values = new HashSet<>();
					for (Object o : EnumSet.allOf(((Class<Enum>) (type)))) {
						values.add(o.toString());
					}
				}else {
				}


		return completer;
	}

	public void setStrict(final boolean strict) {
		this.strict = strict;
	}

	public boolean getStrict() {
		return this.strict;
	}

	public int complete(final Session session, final CommandLine list, final List<String> candidates) {
		int argpos = list.getArgumentPosition();
		int argIndex = list.getCursorArgumentIndex();
		CommandSession commandSession = CommandSessionHolder.getSession();
		if (commandSession != null) {
			commandSession.put(ArgumentCompleter.ARGUMENTS_LIST, list);
		}
		Completer comp = null;
		String[] args = list.getArguments();
		int index = 0;
		if (index < argIndex) {
			if ((!(Session.SCOPE_GLOBAL.equals(scope))) && (!(session.resolveCommand(args[index]).equals((((scope) + ":") + (name)))))) {
				return -1;
			}
			if (!(verifyCompleter(commandCompleter, args[index]))) {
				return -1;
			}
			index++;
		}else {
			comp = commandCompleter;
		}
		if (comp == null) {
			while ((index < argIndex) && (args[index].startsWith("-"))) {
				if (!(verifyCompleter(optionsCompleter, args[index]))) {
					return -1;
				}
				Option option = options.get(args[index]);
				if (option == null) {
					return -1;
				}
				Field field = fields.get(option);
				if (((field != null) && ((field.getType()) != (boolean.class))) && ((field.getType()) != (Boolean.class))) {
					if ((++index) == argIndex) {
					}
				}
				index++;
			} 
			if ((((comp == null) && (index >= argIndex)) && (index < (args.length))) && (args[index].startsWith("-"))) {
				comp = optionsCompleter;
			}
		}
		int lastAgurmentIndex = argIndex - 1;
		if (lastAgurmentIndex >= 1) {
			Option lastOption = options.get(args[lastAgurmentIndex]);
			if (lastOption != null) {
				Field lastField = fields.get(lastOption);
				if (((lastField != null) && ((lastField.getType()) != (boolean.class))) && ((lastField.getType()) != (Boolean.class))) {
					Option option = lastField.getAnnotation(Option.class);
					if (option != null) {
						Completer optionValueCompleter = null;
						String name = option.name();
						if (((optionalCompleters) != null) && (name != null)) {
							optionValueCompleter = optionalCompleters.get(name);
							if (optionValueCompleter == null) {
								String[] aliases = option.aliases();
								if ((aliases.length) > 0) {
									for (int i = 0; (i < (aliases.length)) && (optionValueCompleter == null); i++) {
										optionValueCompleter = optionalCompleters.get(option.aliases()[i]);
									}
								}
							}
						}
						if (optionValueCompleter != null) {
							comp = optionValueCompleter;
						}
					}
				}
			}
		}
		if (comp == null) {
			int indexArg = 0;
			while (index < argIndex) {
				Completer sub = argsCompleters.get((indexArg >= (argsCompleters.size()) ? (argsCompleters.size()) - 1 : indexArg));
				if (!(verifyCompleter(sub, args[index]))) {
					return -1;
				}
				index++;
				indexArg++;
			} 
			comp = argsCompleters.get((indexArg >= (argsCompleters.size()) ? (argsCompleters.size()) - 1 : indexArg));
		}
		String buffer = list.getBuffer();
		int cursor = list.getBufferPosition();
		if (((buffer != null) && (cursor != (buffer.length()))) && (isDelimiter(buffer, cursor))) {
			for (int i = 0; i < (candidates.size()); i++) {
				String val = candidates.get(i);
				while (((val.length()) > 0) && (isDelimiter(val, ((val.length()) - 1)))) {
					val = val.substring(0, ((val.length()) - 1));
				} 
				candidates.set(i, val);
			}
		}
		return 0;
	}

	protected boolean verifyCompleter(Completer completer, String argument) {
		List<String> candidates = new ArrayList<>();
		return false;
	}

	public boolean isDelimiter(final String buffer, final int pos) {
		return (!(isEscaped(buffer, pos))) && (isDelimiterChar(buffer, pos));
	}

	public boolean isEscaped(final String buffer, final int pos) {
		return ((pos > 0) && ((buffer.charAt(pos)) == '\\')) && (!(isEscaped(buffer, (pos - 1))));
	}

	public boolean isDelimiterChar(String buffer, int pos) {
		return Character.isWhitespace(buffer.charAt(pos));
	}

	public static class ProxyServiceCompleter implements Completer {
		private final BundleContext context;

		private final Class<? extends Completer> clazz;

		public ProxyServiceCompleter(BundleContext context, Class<? extends Completer> clazz) {
			this.context = context;
			this.clazz = clazz;
		}

		public int complete(String buffer, int cursor, List<String> candidates) {
			ServiceReference<? extends Completer> ref = context.getServiceReference(clazz);
			if (ref != null) {
				Completer completer = context.getService(ref);
				if (completer != null) {
					try {
					} finally {
						context.ungetService(ref);
					}
				}
			}
			return -1;
		}
	}
}

