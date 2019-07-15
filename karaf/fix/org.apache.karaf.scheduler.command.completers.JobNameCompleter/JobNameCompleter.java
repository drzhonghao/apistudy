

import java.util.List;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;


@Service
public class JobNameCompleter implements Completer {
	@Override
	public int complete(Session session, CommandLine commandLine, List<String> candidates) {
		StringsCompleter delegate = new StringsCompleter();
		try {
		} catch (Exception e) {
		}
		return delegate.complete(session, commandLine, candidates);
	}
}

