import org.apache.karaf.diagnostic.internal.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.karaf.diagnostic.core.DumpProvider;
import org.apache.karaf.diagnostic.management.internal.DiagnosticDumpMBeanImpl;
import org.apache.karaf.diagnostic.common.FeaturesDumpProvider;
import org.apache.karaf.diagnostic.common.LogDumpProvider;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private List<ServiceRegistration<DumpProvider>> registrations;
    private ServiceRegistration<DumpProvider> featuresProviderRegistration;
    private ServiceRegistration mbeanRegistration;
    private SingleServiceTracker<FeaturesService> featuresServiceTracker;
    private ServiceTracker<DumpProvider, DumpProvider> providersTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        registrations = new ArrayList<>();
        registrations.add(context.registerService(DumpProvider.class, new LogDumpProvider(context), null));

        featuresServiceTracker = new SingleServiceTracker<>(context, FeaturesService.class, (oldFs, newFs) -> {
            if (featuresProviderRegistration != null) {
                featuresProviderRegistration.unregister();
                featuresProviderRegistration = null;
            }
            if (newFs != null) {
                featuresProviderRegistration = context.registerService(
                        DumpProvider.class,
                        new FeaturesDumpProvider(newFs),
                        null);
            }
        });
        featuresServiceTracker.open();

        final DiagnosticDumpMBeanImpl diagnostic = new DiagnosticDumpMBeanImpl();
        diagnostic.setBundleContext(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("jmx.objectname", "org.apache.karaf:type=diagnostic,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = context.registerService(
                getInterfaceNames(diagnostic),
                diagnostic,
                props
        );
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        mbeanRegistration.unregister();
        featuresServiceTracker.close();
        for (ServiceRegistration<DumpProvider> reg : registrations) {
            reg.unregister();
        }
    }

    private String[] getInterfaceNames(Object object) {
        List<String> names = new ArrayList<>();
        for (Class cl = object.getClass(); cl != Object.class; cl = cl.getSuperclass()) {
            addSuperInterfaces(names, cl);
        }
        return names.toArray(new String[names.size()]);
    }

    private void addSuperInterfaces(List<String> names, Class clazz) {
        for (Class cl : clazz.getInterfaces()) {
            names.add(cl.getName());
            addSuperInterfaces(names, cl);
        }
    }

}
