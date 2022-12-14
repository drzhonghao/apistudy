import org.apache.karaf.shell.commands.impl.info.PojoInfoProvider;
import org.apache.karaf.shell.commands.impl.info.*;


import org.apache.karaf.shell.commands.info.InfoProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Bundle tracker which check manifest headers for information.
 */
public class InfoBundleTrackerCustomizer implements BundleTrackerCustomizer<ServiceRegistration<InfoProvider>> {

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(InfoBundleTrackerCustomizer.class);

    /**
     * {@inheritDoc}
     */
    public ServiceRegistration<InfoProvider> addingBundle(Bundle bundle, BundleEvent event) {
        Dictionary headers = bundle.getHeaders();
        String headerEntry = (String) headers.get("Karaf-Info");

        InfoProvider provider = createInfo(headerEntry);
        if (provider == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Ignore incorrect info {} provided by bundle {}",
                        headerEntry, bundle.getSymbolicName());
            }
            return null;
        }
        return bundle.getBundleContext().registerService(InfoProvider.class,
                provider, null);
    }

    public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<InfoProvider> object) {
    }

    public void removedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<InfoProvider> object) {
        object.unregister();
    }

    private InfoProvider createInfo(String entry) {
        if (entry == null) {
            return null;
        }

        StringTokenizer tokenizer = new StringTokenizer(entry, ";=");
        if (tokenizer.countTokens() < 3) {
            return null;
        }

        String name = tokenizer.nextToken();
        Properties properties = new Properties();

        do {
            String property = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                properties.put(property, tokenizer.nextElement());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ignore property {} without value", property);
                }
            }
        } while (tokenizer.hasMoreTokens());

        return new PojoInfoProvider(name, properties);
    }

}
