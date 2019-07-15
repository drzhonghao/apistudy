import org.apache.karaf.diagnostic.common.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.apache.karaf.util.StreamUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Dump provider which copies log files from data/log directory to
 * destination.
 */
public class LogDumpProvider implements DumpProvider {

    private final BundleContext bundleContext;

    public LogDumpProvider(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Attach log entries from directory.
     */
    public void createDump(DumpDestination destination) throws Exception {
        // get the ConfigAdmin service
        ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (ref == null) {
            return;
        }

        // get the PAX Logging configuration
        ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) bundleContext.getService(ref);
        try {
            Configuration configuration = configurationAdmin.getConfiguration("org.ops4j.pax.logging", null);

            // get the ".file" Pax Logging properties
            Dictionary dictionary = configuration.getProperties();
            for (Enumeration e = dictionary.keys(); e.hasMoreElements(); ) {
                String property = (String) e.nextElement();
                if (property.endsWith(".fileName")) {
                    // it's a file appender, get the file location
                    String location = (String) dictionary.get(property);
                    File file = new File(location);
                    if (file.exists()) {
                        FileInputStream inputStream = new FileInputStream(file);
                        OutputStream outputStream = destination.add("log/" + file.getName());
                        StreamUtils.copy(inputStream, outputStream);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            bundleContext.ungetService(ref);
        }
    }

}
