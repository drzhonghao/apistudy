import org.apache.karaf.config.command.*;


import java.util.Arrays;

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.Session;

/**
 * Abstract class from which all commands related to the ConfigurationAdmin
 * service should derive.
 * This command retrieves a reference to the ConfigurationAdmin service before
 * calling another method to actually process the command.
 */
public abstract class ConfigCommandSupport implements Action {

    public static final String PROPERTY_CONFIG_PID = "ConfigCommand.PID";
    public static final String PROPERTY_CONFIG_PROPS = "ConfigCommand.Props";
    public static final String PROPERTY_FACTORY = "ConfigCommand.Factory";
    public static final String PROPERTY_ALIAS = "ConfigCommand.Alias";

    @Reference
    protected ConfigRepository configRepository;

    @Reference
    protected Session session;

    @Override
    public Object execute() throws Exception {
        return doExecute();
    }

    protected abstract Object doExecute() throws Exception;

    @SuppressWarnings("rawtypes")
    protected TypedProperties getEditedProps() throws Exception {
        return (TypedProperties) this.session.get(PROPERTY_CONFIG_PROPS);
    }

    public void setConfigRepository(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    protected String displayValue(Object value) {
        if (value == null) {
            return "<null>";
        }
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }
        return value.toString();
    }
}
