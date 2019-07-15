import org.apache.karaf.log.command.*;


import org.apache.karaf.log.core.LogService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

@Command(scope = "log", name = "exception-display", description = "Displays the last occurred exception from the log.")
@Service
public class DisplayException implements Action {

    @Argument(index = 0, name = "logger", description = "The name of the logger. This can be ROOT, ALL, or the name of a logger specified in the org.ops4j.pax.logger.cfg file.", required = false, multiValued = false)
    String logger;

    @Reference
    LogService logService;

    @Override
    public Object execute() throws Exception {
        PaxLoggingEvent throwableEvent = logService.getLastException(logger);
        if (throwableEvent != null) {
            for (String r : throwableEvent.getThrowableStrRep()) {
                System.out.println(r);
            }
            System.out.println();
        }
        return null;
    }

}
