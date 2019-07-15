

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "http", name = "proxy-remove", description = "Remove an existing HTTP proxy")
@Service
public class ProxyRemoveCommand implements Action {}

