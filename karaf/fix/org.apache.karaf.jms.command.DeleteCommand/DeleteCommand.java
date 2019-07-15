

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jms", name = "delete", description = "Delete a JMS connection factory")
@Service
public class DeleteCommand {
	public Object execute() throws Exception {
		return null;
	}
}

