import org.apache.cassandra.service.*;


import java.util.ArrayList;
import java.util.List;

import io.netty.util.concurrent.FastThreadLocal;
import org.apache.cassandra.concurrent.ExecutorLocal;
import org.apache.cassandra.utils.FBUtilities;

public class ClientWarn implements ExecutorLocal<ClientWarn.State>
{
    private static final String TRUNCATED = " [truncated]";
    private static final FastThreadLocal<State> warnLocal = new FastThreadLocal<>();
    public static ClientWarn instance = new ClientWarn();

    private ClientWarn()
    {
    }

    public State get()
    {
        return warnLocal.get();
    }

    public void set(State value)
    {
        warnLocal.set(value);
    }

    public void warn(String text)
    {
        State state = warnLocal.get();
        if (state != null)
            state.add(text);
    }

    public void captureWarnings()
    {
        warnLocal.set(new State());
    }

    public List<String> getWarnings()
    {
        State state = warnLocal.get();
        if (state == null || state.warnings.isEmpty())
            return null;
        return state.warnings;
    }

    public void resetWarnings()
    {
        warnLocal.remove();
    }

    public static class State
    {
        private final List<String> warnings = new ArrayList<>();

        private void add(String warning)
        {
            if (warnings.size() < FBUtilities.MAX_UNSIGNED_SHORT)
                warnings.add(maybeTruncate(warning));
        }

        private static String maybeTruncate(String warning)
        {
            return warning.length() > FBUtilities.MAX_UNSIGNED_SHORT
                   ? warning.substring(0, FBUtilities.MAX_UNSIGNED_SHORT - TRUNCATED.length()) + TRUNCATED
                   : warning;
        }

    }
}
