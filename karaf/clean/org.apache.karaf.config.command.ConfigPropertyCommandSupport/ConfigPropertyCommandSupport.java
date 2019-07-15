import org.apache.karaf.config.command.ConfigCommandSupport;
import org.apache.karaf.config.command.*;


import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.command.completers.ConfigurationCompleter;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;

/**
 * Abstract class which commands that are related to property processing should extend.
 */
public abstract class ConfigPropertyCommandSupport extends ConfigCommandSupport {

    @Option(name = "-p", aliases = "--pid", description = "The configuration pid", required = false, multiValued = false)
    @Completion(ConfigurationCompleter.class)
    protected String pid;

    protected Object doExecute() throws Exception {
        TypedProperties props = getEditedProps();
        if (props == null && pid == null) {
            System.err.println("No configuration is being edited--run the edit command first");
        } else {
            if (props == null) {
                props = new TypedProperties();
            }
            propertyAction(props);
            if(requiresUpdate(pid)) {
                this.configRepository.update(pid, props);
            }
        }
        return null;
    }

    /**
     * Perform an action on the properties.
     *
     * @param props the dictionary where to apply the action.
     */
    protected abstract void propertyAction(TypedProperties props);

    /**
     * Check if the configuration requires to be updated.
     * The default behavior is to update if a valid pid has been passed to the method.
     *
     * @param pid the PID to check.
     * @return <code>true</code> if the configuration requires an update, <code>false</code> else.
     */
    protected boolean requiresUpdate(String pid) {
        return pid != null;
    }

    /**
     * Retrieve configuration from the pid, if used or delegates to session from getting the configuration.
     *
     * @return the edited dictionary.
     * @throws Exception in case of configuration failure.
     */
    @Override
    protected TypedProperties getEditedProps() throws Exception {
        if (pid != null) {
            return this.configRepository.getConfig(pid);
        }
        else {
            return super.getEditedProps();
        }
    }

}
