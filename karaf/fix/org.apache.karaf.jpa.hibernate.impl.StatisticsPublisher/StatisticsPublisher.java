

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.persistence.EntityManagerFactory;
import org.hibernate.stat.Statistics;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StatisticsPublisher implements ServiceTrackerCustomizer<EntityManagerFactory, EntityManagerFactory> {
	private static Logger LOG = LoggerFactory.getLogger(StatisticsPublisher.class);

	private BundleContext context;

	private MBeanServer mbeanServer;

	private ServiceTracker<EntityManagerFactory, EntityManagerFactory> emfTracker;

	public StatisticsPublisher(BundleContext context, MBeanServer mbeanServer) {
		this.context = context;
		this.mbeanServer = mbeanServer;
		this.emfTracker = new ServiceTracker<>(context, EntityManagerFactory.class, this);
	}

	public void start() {
		this.emfTracker.open(true);
	}

	public void stop() {
		ServiceReference<EntityManagerFactory>[] emfRefs = this.emfTracker.getServiceReferences();
		for (ServiceReference<EntityManagerFactory> emfRef : emfRefs) {
			try {
				this.mbeanServer.unregisterMBean(getOName(emfRef));
			} catch (Exception e) {
			}
		}
		this.emfTracker.close();
	}

	ObjectName getOName(ServiceReference<EntityManagerFactory> reference) {
		try {
			String unitName = ((String) (reference.getProperty("osgi.unit.name")));
			return new ObjectName("org.hibernate.statistics", "unitName", unitName);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private void publishStatistics(ServiceReference<EntityManagerFactory> reference, EntityManagerFactory emf) {
		String persitenceProvider = ((String) (reference.getProperty("osgi.unit.provider")));
		if (!("org.hibernate.ejb.HibernatePersistence".equals(persitenceProvider))) {
			return;
		}
		if ((reference.getProperty("org.apache.aries.jpa.proxy.factory")) != null) {
			return;
		}
	}

	@Override
	public EntityManagerFactory addingService(ServiceReference<EntityManagerFactory> reference) {
		EntityManagerFactory emf = context.getService(reference);
		publishStatistics(reference, emf);
		return emf;
	}

	private Object getStatisticsMBean(final Statistics statistics) {
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<EntityManagerFactory> reference, EntityManagerFactory service) {
	}

	@Override
	public void removedService(ServiceReference<EntityManagerFactory> reference, EntityManagerFactory service) {
		try {
			mbeanServer.unregisterMBean(getOName(reference));
		} catch (Exception e) {
		}
	}
}

