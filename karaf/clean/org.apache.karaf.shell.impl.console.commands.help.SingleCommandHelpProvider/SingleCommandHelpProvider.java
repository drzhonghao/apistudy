import org.apache.karaf.shell.impl.console.commands.help.*;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.karaf.shell.api.console.Session;

public class SingleCommandHelpProvider implements HelpProvider {

    public String getHelp(Session session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("command|")) {
                path = path.substring("command|".length());
            } else {
                return null;
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true);
        Session s = session.getFactory().create(bais, ps, ps, session);
        s.put(Session.SCOPE, session.get(Session.SCOPE));
        s.put(Session.SUBSHELL, session.get(Session.SUBSHELL));
        try {
            s.execute(path + " --help");
        } catch (Throwable t) {
            return null;
        } finally {
            s.close();
        }
        return baos.toString();
    }

}
