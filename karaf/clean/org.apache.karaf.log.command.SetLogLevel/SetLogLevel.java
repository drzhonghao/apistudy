import org.apache.karaf.log.command.*;



import org.apache.karaf.log.core.LogService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * Set the log level for a given logger
 */
@Command(scope = "log", name = "set", description = "Sets the log level.")
@Service
public class SetLogLevel implements Action {
    
    @Argument(index = 0, name = "level", description = "The log level to set (TRACE, DEBUG, INFO, WARN, ERROR) or DEFAULT to unset", required = true, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "OFF", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "DEFAULT" })
    String level;

    @Argument(index = 1, name = "logger", description = "Logger name or ROOT (default)", required = false, multiValued = false)
    String logger;

    @Reference
    LogService logService;

    @Override
    public Object execute() throws Exception {
        logService.setLevel(logger, level);
        return null;
    }

}
