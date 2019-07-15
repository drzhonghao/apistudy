import org.apache.karaf.shell.impl.action.osgi.*;


import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.impl.action.command.ManagerImpl;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle extender scanning for command classes.
 */
public class CommandExtender extends AbstractExtender {

    public static final String KARAF_COMMANDS = "Karaf-Commands";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExtender.class);

    //
    // Adapt BundleActivator to make it blueprint friendly
    //

    private Registry registry;

    public CommandExtender(Registry registry) {
        setSynchronous(true);
        this.registry = registry;
        this.registry.register(new ManagerImpl(this.registry, this.registry));
    }

    //
    // Extender implementation
    //

    @Override
    protected Extension doCreateExtension(Bundle bundle) throws Exception {
        if (bundle.getHeaders().get(KARAF_COMMANDS) != null) {
            return new CommandExtension(bundle, registry);
        }
        return null;
    }

    @Override
    protected void debug(Bundle bundle, String msg) {
        StringBuilder buf = new StringBuilder();
        if ( bundle != null )
        {
            buf.append( bundle.getSymbolicName() );
            buf.append( " (" );
            buf.append( bundle.getBundleId() );
            buf.append( "): " );
        }
        buf.append(msg);
        LOGGER.debug(buf.toString());
    }

    @Override
    protected void warn(Bundle bundle, String msg, Throwable t) {
        StringBuilder buf = new StringBuilder();
        if ( bundle != null )
        {
            buf.append( bundle.getSymbolicName() );
            buf.append( " (" );
            buf.append( bundle.getBundleId() );
            buf.append( "): " );
        }
        buf.append(msg);
        LOGGER.warn(buf.toString(), t);
    }

    @Override
    protected void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }

}
