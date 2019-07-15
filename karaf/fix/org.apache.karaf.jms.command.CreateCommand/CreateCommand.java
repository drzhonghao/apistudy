

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;


@Command(scope = "jms", name = "create", description = "Create a JMS connection factory.")
@Service
public class CreateCommand {
	@Argument(index = 0, name = "name", description = "The JMS connection factory name", required = true, multiValued = false)
	String name;

	@Option(name = "-t", aliases = { "--type" }, description = "The JMS connection factory type (ActiveMQ, Artemis or WebsphereMQ)", required = false, multiValued = false)
	@Completion(value = StringsCompleter.class, values = { "activemq", "artemis", "webspheremq" })
	String type = "activemq";

	@Option(name = "--url", description = "URL of the JMS broker. For WebsphereMQ type, the URL is hostname/port/queuemanager/channel", required = false, multiValued = false)
	String url = "tcp://localhost:61616";

	@Option(name = "--pool", description = "The pool mechanism to use for this connection factory", required = false, multiValued = false)
	@Completion(value = StringsCompleter.class, values = { "pooledjms", "narayama", "transx" })
	String pool = "pooledjms";

	@Option(name = "-u", aliases = { "--username" }, description = "Username to connect to the JMS broker", required = false, multiValued = false)
	String username = "karaf";

	@Option(name = "-p", aliases = { "--password" }, description = "Password to connect to the JMS broker", required = false, multiValued = false)
	String password = "karaf";

	public Object execute() throws Exception {
		return null;
	}
}

