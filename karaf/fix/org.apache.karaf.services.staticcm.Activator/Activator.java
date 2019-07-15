

import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


public class Activator implements BundleActivator {
	public static final String CONFIG_DIRS = "org.apache.karaf.services.staticcm.ConfigDirs";

	ServiceRegistration<ConfigurationAdmin> registration;

	@Override
	public void start(BundleContext context) throws Exception {
		List<Configuration> configs = new ArrayList<>();
		String cfgDirs = context.getProperty(Activator.CONFIG_DIRS);
		if (cfgDirs == null) {
			cfgDirs = System.getProperty("karaf.etc");
		}
		for (String dir : cfgDirs.split(",")) {
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if ((registration) != null) {
			registration.unregister();
		}
	}
}

