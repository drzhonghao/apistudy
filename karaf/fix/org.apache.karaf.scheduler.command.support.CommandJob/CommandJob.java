

import org.apache.karaf.shell.api.console.Session;


public class CommandJob {
	private final Session session;

	private final String command;

	public CommandJob(Session session, String command) {
		this.session = session;
		this.command = command;
	}
}

