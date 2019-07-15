import org.apache.karaf.jaas.modules.syncope.SyncopeBackingEngine;
import org.apache.karaf.jaas.modules.syncope.SyncopeLoginModule;
import org.apache.karaf.jaas.modules.syncope.*;


import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SyncopeBackingEngineFactory implements BackingEngineFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncopeBackingEngineFactory.class);

    public BackingEngine build(Map<String, ?> options) {
        SyncopeBackingEngine instance = null;
        String address = (String) options.get(SyncopeLoginModule.ADDRESS);
        String adminUser = (String) options.get(SyncopeLoginModule.ADMIN_USER);
        String adminPassword = (String) options.get(SyncopeLoginModule.ADMIN_PASSWORD);
        String version = (String) options.get(SyncopeLoginModule.VERSION);

        try {
            instance = new SyncopeBackingEngine(address, version, adminUser, adminPassword);
        } catch (Exception e) {
            LOGGER.error("Error creating the Syncope backing engine", e);
        }

        return instance;
    }

    /**
     * Returns the login module class, that this factory can build.
     */
    public String getModuleClass() {
        return SyncopeLoginModule.class.getName();
    }

}
