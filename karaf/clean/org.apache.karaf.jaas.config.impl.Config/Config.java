import org.apache.karaf.jaas.config.impl.Module;
import org.apache.karaf.jaas.config.impl.*;


import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.osgi.framework.BundleContext;

/**
 * An implementation of JaasRealm which is created
 * by the spring namespace handler.
 */
public class Config implements JaasRealm {

    private String name;
    private int rank;
    private Module[] modules;
    private BundleContext bundleContext;
    private transient AppConfigurationEntry[] entries;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Module[] getModules() {
        return modules;
    }

    public void setModules(Module[] modules) {
        this.modules = modules;
        this.entries = null;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public AppConfigurationEntry[] getEntries() {
        if (this.entries == null && this.modules != null) {
            Module[] modules = this.modules;
            AppConfigurationEntry[] entries = new AppConfigurationEntry[modules.length];
            for (int i = 0; i < modules.length; i++) {
                Map<String,Object> options = new HashMap<>();
                // put the bundle context in the options map
                // it's required to be able to use the encryption service
                // in the AbstractKarafLoginModule
                options.put(BundleContext.class.getName(), bundleContext);
                if (modules[i].getOptions() != null) {
                    for (Map.Entry e : modules[i].getOptions().entrySet()) {
                        options.put(e.getKey().toString(), e.getValue());
                    }
                }
                options.put(ProxyLoginModule.PROPERTY_MODULE, modules[i].getClassName());
                options.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
                entries[i] = new AppConfigurationEntry(ProxyLoginModule.class.getName(),
                                                       getControlFlag(modules[i].getFlags()),
                                                       options);
            }
            this.entries = entries;
        }
        return this.entries;
    }

    private AppConfigurationEntry.LoginModuleControlFlag getControlFlag(String flags) {
        if ("required".equalsIgnoreCase(flags)) {
            return AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
        }
        if ("optional".equalsIgnoreCase(flags)) {
            return AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
        }
        if ("requisite".equalsIgnoreCase(flags)) {
            return AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
        }
        if ("sufficient".equalsIgnoreCase(flags)) {
            return AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
        }
        return null;
    }
}
