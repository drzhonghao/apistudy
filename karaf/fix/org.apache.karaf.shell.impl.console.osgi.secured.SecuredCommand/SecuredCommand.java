

import java.util.List;
import org.apache.felix.gogo.runtime.Closure;
import org.apache.felix.gogo.runtime.Token;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Function;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.impl.console.osgi.secured.SecuredSessionFactoryImpl;


public class SecuredCommand implements Command , Function {
	private final SecuredSessionFactoryImpl factory;

	private final Command command;

	public SecuredCommand(SecuredSessionFactoryImpl factory, Command command) {
		this.command = command;
		this.factory = factory;
	}

	public String getScope() {
		return command.getScope();
	}

	public String getName() {
		return command.getName();
	}

	@Override
	public String getDescription() {
		return command.getDescription();
	}

	@Override
	public Completer getCompleter(boolean scoped) {
		return command.getCompleter(scoped);
	}

	@Override
	public Parser getParser() {
		return null;
	}

	@Override
	public Object execute(Session session, List<Object> arguments) throws Exception {
		return command.execute(session, arguments);
	}

	public Object execute(final CommandSession commandSession, List<Object> arguments) throws Exception {
		Session session = ((Session) (commandSession.get(".session")));
		for (int i = 0; i < (arguments.size()); i++) {
			Object v = arguments.get(i);
			if (v instanceof Closure) {
				final Closure closure = ((Closure) (v));
				arguments.set(i, new SecuredCommand.VersatileFunction(closure));
			}
			if (v instanceof Token) {
				arguments.set(i, v.toString());
			}
		}
		return execute(session, arguments);
	}

	static class VersatileFunction implements org.apache.felix.service.command.Function , Function {
		private final Closure closure;

		VersatileFunction(Closure closure) {
			this.closure = closure;
		}

		@Override
		public Object execute(CommandSession commandSession, List<Object> list) throws Exception {
			return closure.execute(commandSession, list);
		}

		@Override
		public Object execute(Session session, List<Object> arguments) throws Exception {
			CommandSession commandSession = ((CommandSession) (session.get(".commandSession")));
			return closure.execute(commandSession, arguments);
		}

		@Override
		public String toString() {
			return closure.toString();
		}
	}
}

