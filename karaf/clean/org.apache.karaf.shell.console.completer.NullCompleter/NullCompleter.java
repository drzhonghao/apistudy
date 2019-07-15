import org.apache.karaf.shell.console.completer.*;


import java.util.List;

import org.apache.karaf.shell.console.Completer;

@Deprecated
public class NullCompleter implements Completer {

    public static final NullCompleter INSTANCE = new NullCompleter();

    public int complete(String buffer, int cursor, List<String> candidates) {
        return -1;
    }
}
