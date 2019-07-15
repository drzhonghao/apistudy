

import javax.management.MBeanServer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class Activator implements BundleActivator , ServiceTrackerCustomizer<MBeanServer, MBeanServer> {
	private ServiceTracker<MBeanServer, MBeanServer> mbeanServerTracker;

	private BundleContext context;

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		mbeanServerTracker = new ServiceTracker<>(context, MBeanServer.class, this);
		mbeanServerTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		mbeanServerTracker.close();
	}

	@Override
	public MBeanServer addingService(ServiceReference<MBeanServer> reference) {
		MBeanServer mbeanServer = context.getService(reference);
		return mbeanServer;
	}

	@Override
	public void modifiedService(ServiceReference<MBeanServer> reference, MBeanServer service) {
	}

	@Override
	public void removedService(ServiceReference<MBeanServer> reference, MBeanServer service) {
	}
}

