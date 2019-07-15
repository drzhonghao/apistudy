import org.apache.karaf.jaas.config.impl.*;


import java.util.List;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.slf4j.LoggerFactory;

public class OsgiConfiguration extends Configuration {

    private final List<JaasRealm> realms = new CopyOnWriteArrayIdentityList<>();
    private Configuration defaultConfiguration;

    public void init() {
        try {
            defaultConfiguration = Configuration.getConfiguration();
        } catch (Throwable ex) {
            // default configuration for fallback could not be retrieved
            LoggerFactory.getLogger(OsgiConfiguration.class).warn("Unable to retrieve default configuration", ex);
        }
        Configuration.setConfiguration(this);
    }

    public void close() {
        realms.clear();
        Configuration.setConfiguration(defaultConfiguration);
    }

    public void register(JaasRealm realm, Map<String,?> properties) {
        if (realm != null) {
            realms.add(realm);
        }
    }

    public void unregister(JaasRealm realm, Map<String,?> properties) {
        if (realm != null) {
            realms.remove(realm);
        }
    }

    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        JaasRealm realm = null;
        for (JaasRealm r : realms) {
            if (r.getName().equals(name)) {
                if (realm == null || r.getRank() > realm.getRank()) {
                    realm = r;
                }
            }
        }
        if (realm != null) {
            return realm.getEntries();
        } else if (defaultConfiguration != null) {
            return defaultConfiguration.getAppConfigurationEntry(name);
        }
        return null;
    }

    public void refresh() {
        if (defaultConfiguration != null) {
            defaultConfiguration.refresh();
        }
    }
}
