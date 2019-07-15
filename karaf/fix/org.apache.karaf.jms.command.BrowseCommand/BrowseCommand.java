

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;


@Command(scope = "jms", name = "browse", description = "Browse a JMS queue")
@Service
public class BrowseCommand {
	@Argument(index = 1, name = "queue", description = "The JMS queue to browse", required = true, multiValued = false)
	String queue;

	@Option(name = "-s", aliases = { "--selector" }, description = "The selector to select the messages to browse", required = false, multiValued = false)
	String selector;

	@Option(name = "-v", aliases = { "--verbose" }, description = "Display JMS properties", required = false, multiValued = false)
	boolean verbose = false;

	public Object execute() throws Exception {
		ShellTable table = new ShellTable();
		table.column("Message ID");
		table.column("Content").maxSize(80);
		table.column("Charset");
		table.column("Type");
		table.column("Correlation ID");
		table.column("Delivery Mode");
		table.column("Destination");
		table.column("Expiration");
		table.column("Priority");
		table.column("Redelivered");
		table.column("ReplyTo");
		table.column("Timestamp");
		if (verbose) {
			table.column("Properties");
		}
		table.print(System.out);
		return null;
	}
}

