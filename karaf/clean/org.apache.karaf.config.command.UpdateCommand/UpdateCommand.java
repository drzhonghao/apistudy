import org.apache.karaf.config.command.ConfigCommandSupport;
import org.apache.karaf.config.command.*;


import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "config", name = "update", description = "Saves and propagates changes from the configuration being edited.")
@Service
public class UpdateCommand extends ConfigCommandSupport {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object doExecute() throws Exception {
        TypedProperties props = getEditedProps();
        if (props == null) {
            System.err.println("No configuration is being edited. Run the edit command first.");
            return null;
        }

        String pid = (String) this.session.get(PROPERTY_CONFIG_PID);
        boolean isFactory = this.session.get(PROPERTY_FACTORY) != null && (Boolean) this.session.get(PROPERTY_FACTORY);
        if (isFactory) {
            String alias = (String) this.session.get(PROPERTY_ALIAS);
            this.configRepository.createFactoryConfiguration(pid, alias, props);
        } else {
        	this.configRepository.update(pid, props);
        }
        this.session.put(PROPERTY_CONFIG_PID, null);
        this.session.put(PROPERTY_FACTORY, null);
        this.session.put(PROPERTY_CONFIG_PROPS, null);
        if (this.session.get(PROPERTY_ALIAS) != null) {
            this.session.put(PROPERTY_ALIAS, null);
        }
        return null;
    }
}
