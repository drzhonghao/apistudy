

import java.util.List;
import org.apache.felix.gogo.runtime.Closure;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Function;
import org.apache.karaf.shell.api.console.Session;


public class CommandWrapper implements Function {
	private final Command command;

	public CommandWrapper(Command command) {
		this.command = command;
	}

	public Command getCommand() {
		return command;
	}

	public Object execute(final CommandSession commandSession, List<Object> arguments) throws Exception {
		Session session = ((Session) (commandSession.get(".session")));
		for (int i = 0; i < (arguments.size()); i++) {
			Object v = arguments.get(i);
			if (v instanceof Closure) {
				final Closure closure = ((Closure) (v));
				arguments.set(i, ((Function) (( s, a) -> closure.execute(commandSession, a))));
			}
		}
		return command.execute(session, arguments);
	}
}

