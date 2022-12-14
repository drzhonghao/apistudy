import org.apache.karaf.config.command.completers.*;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Destroy;
import org.apache.karaf.shell.api.action.lifecycle.Init;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/**
 * {@link Completer} for Configuration Admin configurations.
 *
 * Displays a list of existing config instance configurations for completion.
 *
 */
@Service
public class ConfigurationCompleter implements Completer, ConfigurationListener {

    private final StringsCompleter delegate = new StringsCompleter();

    @Reference
    private ConfigurationAdmin admin;

    @Reference
    private BundleContext bundleContext;

    private ServiceRegistration<ConfigurationListener> registration;

    public void setAdmin(ConfigurationAdmin admin) {
        this.admin = admin;
    }

    @Init
    public void init() {
        registration = bundleContext.registerService(ConfigurationListener.class, this, null);

        Configuration[] configs;
        try {
            configs = admin.listConfigurations(null);
            if (configs == null) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        Collection<String> pids = new ArrayList<>();
        for (Configuration config : configs) {
            pids.add(config.getPid());
        }

        delegate.getStrings().addAll(pids);
    }

    @Destroy
    public void destroy() {
        registration.unregister();
    }

    public int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
        return delegate.complete(session, commandLine, candidates);
    }

    public void configurationEvent(ConfigurationEvent configurationEvent) {
        String pid = configurationEvent.getPid();
        if (configurationEvent.getType() == ConfigurationEvent.CM_DELETED) {
            delegate.getStrings().remove(pid);
        } else if (configurationEvent.getType() == ConfigurationEvent.CM_UPDATED) {
            delegate.getStrings().add(pid);
        }
    }
}
