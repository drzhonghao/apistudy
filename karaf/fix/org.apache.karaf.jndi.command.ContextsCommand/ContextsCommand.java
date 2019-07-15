

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jndi", name = "contexts", description = "List the JNDI sub-contexts.")
@Service
public class ContextsCommand implements Action {}

