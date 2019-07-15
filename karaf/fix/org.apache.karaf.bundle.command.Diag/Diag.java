

import org.apache.karaf.bundle.command.BundlesCommand;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;


@Command(scope = "bundle", name = "diag", description = "Displays diagnostic information why a bundle is not Active")
@Service
public class Diag extends BundlesCommand {
	@Override
	protected void executeOnBundle(Bundle bundle) throws Exception {
	}
}

