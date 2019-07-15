

import java.util.List;
import org.ops4j.pax.web.service.spi.WarManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WebContainerServiceImpl implements BundleListener {
	private BundleContext bundleContext;

	private WarManager warManager;

	private static final Logger LOGGER = LoggerFactory.getLogger(WebContainerServiceImpl.class);

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setWarManager(WarManager warManager) {
		this.warManager = warManager;
	}

	@Override
	public void bundleChanged(BundleEvent bundleEvent) {
		if ((((bundleEvent.getType()) == (BundleEvent.UNINSTALLED)) || ((bundleEvent.getType()) == (BundleEvent.UNRESOLVED))) || ((bundleEvent.getType()) == (BundleEvent.STOPPED))) {
		}
	}

	public void stop(List<Long> bundleIds) throws Exception {
		if ((bundleIds != null) && (!(bundleIds.isEmpty()))) {
			for (long bundleId : bundleIds) {
			}
		}
	}

	public String state(long bundleId) {
		String topic = "Unknown    ";
		while ((topic.length()) < 11) {
			topic += " ";
		} 
		return topic;
	}

	private String getStateString(Bundle bundle) {
		int state = bundle.getState();
		if (state == (Bundle.ACTIVE)) {
			return "Active     ";
		}else
			if (state == (Bundle.INSTALLED)) {
				return "Installed  ";
			}else
				if (state == (Bundle.RESOLVED)) {
					return "Resolved   ";
				}else
					if (state == (Bundle.STARTING)) {
						return "Starting   ";
					}else
						if (state == (Bundle.STOPPING)) {
							return "Stopping   ";
						}else {
							return "Unknown    ";
						}




	}

	public String getWebContextPath(Long id) {
		return null;
	}
}

