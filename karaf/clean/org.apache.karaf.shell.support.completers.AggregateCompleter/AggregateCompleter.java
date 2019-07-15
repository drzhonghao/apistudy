import org.apache.karaf.shell.support.completers.*;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;


/**
 * Completer which contains multiple completers and aggregates them together.
 */
public class AggregateCompleter implements Completer
{
    private final Collection<Completer> completers;

    public AggregateCompleter(final Collection<Completer> completers) {
        assert completers != null;
        this.completers = completers;
    }

    public int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
        List<Candidate> cands = new ArrayList<>();
        completeCandidates(session, commandLine, cands);
        for (Candidate cand : cands) {
            candidates.add(cand.value());
        }
        return 0;
    }

    @Override
    public void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
        // buffer could be null
        assert candidates != null;
        for (Completer completer : completers) {
            completer.completeCandidates(session, commandLine, candidates);
        }
    }

}
