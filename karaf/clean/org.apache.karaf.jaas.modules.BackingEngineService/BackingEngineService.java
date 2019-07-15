import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.*;


import org.apache.karaf.jaas.boot.ProxyLoginModule;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.List;

public class BackingEngineService {

    private List<BackingEngineFactory> engineFactories;

    public BackingEngine get(AppConfigurationEntry entry) {

        if (engineFactories != null) {
            for (BackingEngineFactory factory : engineFactories) {
                String loginModuleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
                if (factory.getModuleClass().equals(loginModuleClass)) {
                    return factory.build(entry.getOptions());
                }
            }
        }
        return null;
    }

    public List<BackingEngineFactory> getEngineFactories() {
        return engineFactories;
    }

    public void setEngineFactories(List<BackingEngineFactory> engineFactories) {
        this.engineFactories = engineFactories;
    }

}
