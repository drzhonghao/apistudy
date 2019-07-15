

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Dictionary;
import org.apache.karaf.bundle.command.BundleCommand;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.util.bundles.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;


@Command(scope = "bundle", name = "update", description = "Update bundle.")
@Service
public class Update extends BundleCommand {
	@Argument(index = 1, name = "location", description = "The bundles update location", required = false, multiValued = false)
	URI location;

	@Option(name = "--raw", description = "Do not update the bundles's Bundle-UpdateLocation manifest header")
	boolean raw;

	@Option(name = "-r", aliases = { "--refresh" }, description = "Perform a refresh after the bundle update", required = false, multiValued = false)
	boolean refresh;

	protected Object doExecute(Bundle bundle) throws Exception {
		if ((location) != null) {
			update(bundle, location.toURL());
		}else {
			String loc = bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
			if ((loc != null) && (!(loc.equals(bundle.getLocation())))) {
				update(bundle, new URL(loc));
			}else {
				bundle.update();
			}
		}
		if (refresh) {
		}
		return null;
	}

	private void update(Bundle bundle, URL location) throws IOException, BundleException {
		try (InputStream is = location.openStream()) {
			if (raw) {
				bundle.update(is);
			}else {
				File file = BundleUtils.fixBundleWithUpdateLocation(is, location.toString());
				try (FileInputStream fis = new FileInputStream(file)) {
					bundle.update(fis);
				}
				file.delete();
			}
		}
	}
}

