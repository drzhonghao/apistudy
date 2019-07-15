import org.apache.karaf.shell.impl.console.osgi.*;


import org.apache.felix.service.command.CommandSessionListener;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracker for CommandSessionListener.
 */
public class ListenerTracker extends ServiceTracker<CommandSessionListener, CommandSessionListener> {

    private SessionFactoryImpl sessionFactory;

    public ListenerTracker(SessionFactoryImpl sessionFactory, BundleContext context) {
        super(context, CommandSessionListener.class, null);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public CommandSessionListener addingService(ServiceReference<CommandSessionListener> reference) {
        CommandSessionListener service = super.addingService(reference);
        sessionFactory.getCommandProcessor().addListener(service);
        return service;
    }

    @Override
    public void removedService(ServiceReference<CommandSessionListener> reference, CommandSessionListener service) {
        sessionFactory.getCommandProcessor().removeListener(service);
        super.removedService(reference, service);
    }
}
