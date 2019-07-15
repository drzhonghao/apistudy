import org.apache.karaf.jaas.command.completers.*;


import java.util.List;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

@Service
public class RealmCompleter implements Completer {

    @Reference
    private List<JaasRealm> realms;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            if (realms != null && !realms.isEmpty())
                for (JaasRealm realm : realms) {
                    delegate.getStrings().add(realm.getName());
                }
        } catch (Exception e) {
            // Ignore
        }
        return delegate.complete(session, commandLine, candidates);
    }

}
