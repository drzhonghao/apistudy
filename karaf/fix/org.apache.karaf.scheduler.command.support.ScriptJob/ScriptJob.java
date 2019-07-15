

import org.apache.karaf.shell.api.console.Function;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ScriptJob {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptJob.class);

	private final SessionFactory sessionFactory;

	private final Session session;

	private final Function script;

	public ScriptJob(SessionFactory sessionFactory, Session session, Function script) {
		this.sessionFactory = sessionFactory;
		this.session = session;
		this.script = script;
	}
}

