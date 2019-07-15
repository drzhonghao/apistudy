import org.apache.karaf.kar.command.completers.*;


import java.util.List;

import org.apache.karaf.kar.KarService;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * Completer on all installed KAR files.
 */
@Service
public class KarCompleter implements Completer {

    @Reference
    private KarService karService;
    
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            for (String karName : karService.list()) {
                delegate.getStrings().add(karName);
            }
        } catch (Exception e) {
            // ignore
        }
        return delegate.complete(session, commandLine, candidates);
    }
    
}
