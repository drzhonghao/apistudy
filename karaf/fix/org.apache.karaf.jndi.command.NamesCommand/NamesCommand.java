

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jndi", name = "names", description = "List the JNDI names.")
@Service
public class NamesCommand implements Action {}

