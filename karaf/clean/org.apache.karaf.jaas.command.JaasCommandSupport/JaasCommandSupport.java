import org.apache.karaf.jaas.command.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.Session;

public abstract class JaasCommandSupport implements Action {

    public static final String JAAS_REALM = "JaasCommand.REALM";
    public static final String JAAS_ENTRY = "JaasCommand.ENTRY";
    public static final String JAAS_CMDS = "JaasCommand.COMMANDS";

    @Reference
    List<BackingEngineFactory> engineFactories;

    @Reference
    List<JaasRealm> realms;

    @Reference
    Session session;

    protected abstract Object doExecute(BackingEngine engine) throws Exception;

    /**
     * Add the command to the command queue.
     */
    @Override
    public Object execute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);
        @SuppressWarnings("unchecked")
        Queue<JaasCommandSupport> commandQueue = (Queue<JaasCommandSupport>) session.get(JAAS_CMDS);

        if (realm != null && entry != null) {
            if (commandQueue != null) {
                commandQueue.add(this);
            }
        } else {
            System.err.println("No JAAS Realm / Module has been selected");
        }
        return null;
    }

    public BackingEngine getBackingEngine(AppConfigurationEntry entry) {
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

    public List<JaasRealm> getRealms() {
        return getRealms(false);
    }

    public List<JaasRealm> getRealms(boolean hidden) {
        if (hidden) {
            return realms;
        } else {
            Map<String, JaasRealm> map = new TreeMap<>();
            for (JaasRealm realm : realms) {
                if (!map.containsKey(realm.getName())
                        || realm.getRank() > map.get(realm.getName()).getRank()) {
                    map.put(realm.getName(), realm);
                }
            }
            return new ArrayList<>(map.values());
        }
    }

    public void setRealms(List<JaasRealm> realms) {
        this.realms = realms;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
