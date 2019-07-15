

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.CommandWithAction;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.compat.OldArgumentCompleter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class CommandTracker implements ServiceTrackerCustomizer<Object, Object> {
	SessionFactory sessionFactory;

	BundleContext context;

	ServiceTracker<?, ?> tracker;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setContext(BundleContext context) {
		this.context = context;
	}

	public void init() throws Exception {
		Filter filter = context.createFilter(String.format("(&(%s=*)(%s=*)(|(%s=%s)(%s=%s)))", CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION, Constants.OBJECTCLASS, CommandWithAction.class.getName(), Constants.OBJECTCLASS, CommandWithAction.class.getName()));
		this.tracker = new ServiceTracker<>(context, filter, this);
		this.tracker.open();
	}

	public void destroy() {
		tracker.close();
	}

	@Override
	public Object addingService(final ServiceReference reference) {
		Object service = context.getService(reference);
		if (service instanceof CommandWithAction) {
			final CommandWithAction oldCommand = ((CommandWithAction) (service));
			final Command command = new Command() {
				@Override
				public String getScope() {
					return reference.getProperty(CommandProcessor.COMMAND_SCOPE).toString();
				}

				@Override
				public String getName() {
					return reference.getProperty(CommandProcessor.COMMAND_FUNCTION).toString();
				}

				@Override
				public String getDescription() {
					final org.apache.karaf.shell.commands.Command cmd = oldCommand.getActionClass().getAnnotation(org.apache.karaf.shell.commands.Command.class);
					if (cmd != null) {
						return cmd.description();
					}
					try {
						Method method = oldCommand.getActionClass().getMethod("description");
						method.setAccessible(true);
						return ((String) (method.invoke(oldCommand.createNewAction())));
					} catch (Throwable ignore) {
					}
					return getName();
				}

				@Override
				public Completer getCompleter(final boolean scoped) {
					return null;
				}

				@Override
				public Parser getParser() {
					return null;
				}

				@Override
				public Object execute(Session session, List<Object> arguments) throws Exception {
					CommandSession commandSession = ((CommandSession) (session.get(".commandSession")));
					return oldCommand.execute(commandSession, arguments);
				}
			};
			sessionFactory.getRegistry().register(command);
			return command;
		}else
			if (service instanceof CommandWithAction) {
				final CommandWithAction oldCommand = ((CommandWithAction) (service));
				final Command command = new Command() {
					@Override
					public String getScope() {
						return reference.getProperty(CommandProcessor.COMMAND_SCOPE).toString();
					}

					@Override
					public String getName() {
						return reference.getProperty(CommandProcessor.COMMAND_FUNCTION).toString();
					}

					@Override
					public String getDescription() {
						final org.apache.felix.gogo.commands.Command cmd = oldCommand.getActionClass().getAnnotation(org.apache.felix.gogo.commands.Command.class);
						if (cmd != null) {
							return cmd.description();
						}
						try {
							Method method = oldCommand.getActionClass().getMethod("description");
							method.setAccessible(true);
							return ((String) (method.invoke(oldCommand.createNewAction())));
						} catch (Throwable ignore) {
						}
						return getName();
					}

					@Override
					public Completer getCompleter(final boolean scoped) {
						final OldArgumentCompleter completer = new OldArgumentCompleter(oldCommand, getScope(), getName(), scoped);
						return completer::complete;
					}

					@Override
					public Parser getParser() {
						return null;
					}

					@Override
					public Object execute(Session session, List<Object> arguments) throws Exception {
						CommandSession commandSession = ((CommandSession) (session.get(".commandSession")));
						return oldCommand.execute(commandSession, arguments);
					}
				};
				sessionFactory.getRegistry().register(command);
				return command;
			}else {
				return null;
			}

	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		if (service instanceof Command) {
			sessionFactory.getRegistry().unregister(service);
		}
		if (service instanceof List) {
			List<Command> commands = ((List<Command>) (service));
			for (Command command : commands) {
				sessionFactory.getRegistry().unregister(command);
			}
		}
		context.ungetService(reference);
	}
}

