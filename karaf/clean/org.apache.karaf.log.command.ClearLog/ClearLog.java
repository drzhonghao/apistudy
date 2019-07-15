import org.apache.karaf.log.command.*;


import org.apache.karaf.log.core.LogService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 * Clear the last log entries.
 */
@Command(scope = "log", name = "clear", description = "Clear log entries.")
@Service
public class ClearLog implements Action {

    @Reference
    LogService logService;

    @Override
    public Object execute() throws Exception {
        logService.clearEvents();
        return null;
    }

}
