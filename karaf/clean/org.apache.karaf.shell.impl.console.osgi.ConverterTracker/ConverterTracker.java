import org.apache.karaf.shell.impl.console.osgi.*;


import org.apache.felix.service.command.Converter;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracker for Converter.
 */
public class ConverterTracker extends ServiceTracker<Converter, Converter> {

    private SessionFactoryImpl sessionFactory;

    public ConverterTracker(SessionFactoryImpl sessionFactory, BundleContext context) {
        super(context, Converter.class, null);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Converter addingService(ServiceReference<Converter> reference) {
        Converter service = super.addingService(reference);
        sessionFactory.getCommandProcessor().addConverter(service);
        return service;
    }

    @Override
    public void removedService(ServiceReference<Converter> reference, Converter service) {
        sessionFactory.getCommandProcessor().removeConverter(service);
        super.removedService(reference, service);
    }
}
