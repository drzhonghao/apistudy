import org.apache.karaf.shell.support.parsing.*;


import java.util.List;

import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;

public class DefaultParser implements Parser {

    @Override
    public CommandLine parse(Session session, String command, int cursor) {
        GogoParser parser = new GogoParser(command, cursor);
        List<String> args = parser.statement();
        return new CommandLineImpl(
                        args.toArray(new String[args.size()]),
                        parser.cursorArgumentIndex(),
                        parser.argumentPosition(),
                        cursor,
                        command.substring(0, parser.position()));
    }

    @Override
    public String preprocess(Session session, CommandLine cmdLine) {
        StringBuilder parsed = new StringBuilder();
        for (int i = 0 ; i < cmdLine.getArguments().length; i++) {
            String arg = cmdLine.getArguments()[i];
            if (i > 0) {
                parsed.append(" ");
            }
            parsed.append(arg);
        }
        return parsed.toString();
    }

}
