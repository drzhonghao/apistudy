

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.TabularData;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(name = ServiceComponentRuntimeMBeanImpl.COMPONENT_NAME, enabled = true, immediate = true, properties = { "org/apache/karaf/scr/management/internal/ServiceComponentRuntimeMBeanImpl.properties" })
public class ServiceComponentRuntimeMBeanImpl extends StandardMBean {
	public static final String OBJECT_NAME = "org.apache.karaf:type=scr,name=" + (System.getProperty("karaf.name", "root"));

	public static final String COMPONENT_NAME = "ServiceComponentRuntimeMBean";

	public static final String COMPONENT_LABEL = "Apache Karaf ServiceComponentRuntime MBean";

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceComponentRuntimeMBeanImpl.class);

	private MBeanServer mBeanServer;

	private BundleContext context;

	private ServiceComponentRuntime scrService;

	private ReadWriteLock lock = new ReentrantReadWriteLock();

	@org.osgi.service.component.annotations.Activate
	public void activate(BundleContext context) throws Exception {
		ServiceComponentRuntimeMBeanImpl.LOGGER.info(("Activating the " + (ServiceComponentRuntimeMBeanImpl.COMPONENT_LABEL)));
		Map<Object, String> mbeans = new HashMap<>();
		mbeans.put(this, "org.apache.karaf:type=scr,name=${karaf.name}");
		try {
			lock.writeLock().lock();
			this.context = context;
			if ((mBeanServer) != null) {
				mBeanServer.registerMBean(this, new ObjectName(ServiceComponentRuntimeMBeanImpl.OBJECT_NAME));
			}
		} catch (Exception e) {
			ServiceComponentRuntimeMBeanImpl.LOGGER.error(("Exception registering the SCR Management MBean: " + (e.getLocalizedMessage())), e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@org.osgi.service.component.annotations.Deactivate
	public void deactivate() throws Exception {
		ServiceComponentRuntimeMBeanImpl.LOGGER.info(("Deactivating the " + (ServiceComponentRuntimeMBeanImpl.COMPONENT_LABEL)));
		try {
			lock.writeLock().lock();
			if ((mBeanServer) != null) {
				mBeanServer.unregisterMBean(new ObjectName(ServiceComponentRuntimeMBeanImpl.OBJECT_NAME));
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public TabularData getComponents() {
		return null;
	}

	public TabularData getComponentConfigs() {
		return null;
	}

	public TabularData getComponentConfigs(long bundleId, String componentName) {
		return null;
	}

	public boolean isComponentEnabled(long bundleId, String componentName) {
		return scrService.isComponentEnabled(findComponent(bundleId, componentName));
	}

	public void enableComponent(long bundleId, String componentName) {
		scrService.enableComponent(findComponent(bundleId, componentName));
	}

	public void disableComponent(long bundleId, String componentName) {
		scrService.disableComponent(findComponent(bundleId, componentName));
	}

	private ComponentDescriptionDTO findComponent(long bundleId, String componentName) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle != null) {
			return scrService.getComponentDescriptionDTO(bundle, componentName);
		}else {
			throw new IllegalArgumentException(("No component found for name: " + componentName));
		}
	}

	@org.osgi.service.component.annotations.Reference
	public void setmBeanServer(MBeanServer mBeanServer) {
		this.mBeanServer = mBeanServer;
	}

	public void unsetmBeanServer(MBeanServer mBeanServer) {
		this.mBeanServer = null;
	}

	@org.osgi.service.component.annotations.Reference
	public void setScrService(ServiceComponentRuntime scrService) {
		this.scrService = scrService;
	}

	public void unsetScrService(ServiceComponentRuntime scrService) {
		this.scrService = null;
	}
}

