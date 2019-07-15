import org.apache.karaf.jaas.modules.publickey.PublickeyBackingEngine;
import org.apache.karaf.jaas.modules.publickey.PublickeyLoginModule;
import org.apache.karaf.jaas.modules.publickey.*;


import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PublickeyBackingEngineFactory implements BackingEngineFactory {

    private final Logger logger = LoggerFactory.getLogger(PublickeyBackingEngineFactory.class);

    private static final String USER_FILE = "users";

    public BackingEngine build(Map<String, ?> options) {
        PublickeyBackingEngine engine = null;
        String usersFile = (String) options.get(USER_FILE);

        File f = new File(usersFile);
        Properties users;
        try {
            users = new Properties(f);
            return new PublickeyBackingEngine(users);
        } catch (IOException ioe) {
            logger.warn("Cannot open keys file:" + usersFile);
        }
        return engine;
    }

    public String getModuleClass() {
        return PublickeyLoginModule.class.getName();
    }

}
