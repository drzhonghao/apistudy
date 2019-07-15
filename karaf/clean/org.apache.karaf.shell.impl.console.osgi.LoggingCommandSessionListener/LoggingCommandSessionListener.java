import org.apache.karaf.shell.impl.console.osgi.*;


import java.util.Collection;
import java.util.Collections;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.CommandSessionListener;
import org.apache.karaf.shell.api.console.CommandLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCommandSessionListener implements CommandSessionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCommandSessionListener.class);

    private Collection<CommandLoggingFilter> filters = Collections.emptyList();

    public Collection<CommandLoggingFilter> getFilters() {
        return filters;
    }

    public void setFilters(Collection<CommandLoggingFilter> filters) {
        this.filters = filters;
    }

    private CharSequence filter(CharSequence command) {
        for (CommandLoggingFilter filter : filters) {
            command = filter.filter(command);
        }
        return command;
    }

    public void beforeExecute(CommandSession session, CharSequence command) {
        if ( LOGGER.isDebugEnabled() ) {
            command = filter(command);
            LOGGER.debug("Executing command: '" + command + "'");
        }
    }

    public void afterExecute(CommandSession session, CharSequence command, Exception exception) {
        if (LOGGER.isDebugEnabled()) {
            command = filter(command);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.debug("Command: '" + command + "' failed", exception);
            } else {
                LOGGER.debug("Command: '" + command + "' failed: " + exception);
            }
        }
    }

    public void afterExecute(CommandSession session, CharSequence command, Object result) {
        if (LOGGER.isDebugEnabled()) {
            command = filter(command);
            LOGGER.debug("Command: '" + command + "' returned '" + result + "'");
        }
    }
}
