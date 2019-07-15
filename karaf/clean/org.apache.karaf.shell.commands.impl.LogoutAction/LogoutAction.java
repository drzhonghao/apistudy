import org.apache.karaf.shell.commands.impl.*;


import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "shell", name = "logout", description = "Disconnects shell from current session.")
@Service
public class LogoutAction implements Action {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        log.info("Disconnecting from current session...");
        session.close();
        return null;
    }

}
