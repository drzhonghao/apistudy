

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "http", name = "proxy-add", description = "Add a new HTTP proxy")
@Service
public class ProxyAddCommand implements Action {
	@Argument(index = 0, name = "url", description = "HTTP proxy URL", required = true, multiValued = false)
	String url;

	@Argument(index = 1, name = "proxyTo", description = "HTTP location to proxy on the prefix", required = true, multiValued = false)
	String proxyTo;
}

