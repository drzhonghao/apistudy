

import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WhiteboardHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private ServiceTracker<?, ?> serviceTracker;

	public void deactivate() {
		this.serviceTracker.close();
	}

	private String getServiceIdentifier(final ServiceReference ref) {
		return null;
	}
}

