import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngine;
import org.apache.karaf.jaas.modules.properties.PropertiesLoginModule;
import org.apache.karaf.jaas.modules.properties.*;


import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PropertiesBackingEngineFactory implements BackingEngineFactory {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(PropertiesBackingEngineFactory.class);

    private static final String USER_FILE = "users";

    /**
     * Builds the Backing Engine
     */
    public BackingEngine build(Map<String,?> options) {
        PropertiesBackingEngine engine = null;
        String usersFile = (String) options.get(USER_FILE);

        File f = new File(usersFile);
        Properties users;
        try {
            users = new Properties(f);
            EncryptionSupport encryptionSupport = new EncryptionSupport(options);
            engine = new PropertiesBackingEngine(users, encryptionSupport);
        } catch (IOException ioe) {
            LOGGER.warn("Cannot open users file: {}", usersFile);
        }
        return engine;
    }

    /**
     * Returns the login module class, that this factory can build.
     */
    public String getModuleClass() {
        return PropertiesLoginModule.class.getName();
    }

}
