

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import org.apache.karaf.jaas.modules.audit.AbstractAuditLoginModule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventAdminAuditLoginModule extends AbstractAuditLoginModule {
	public static final String TOPIC_EVENTS = "org/apache/karaf/login/";

	private static final Logger LOGGER = LoggerFactory.getLogger(EventAdminAuditLoginModule.class);

	private static boolean errorLogged;

	private BundleContext bundleContext;

	private String topic;

	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		super.initialize(subject, callbackHandler, sharedState, options);
		bundleContext = ((BundleContext) (options.get(BundleContext.class.getName())));
		topic = ((String) (options.get("topic")));
		if ((topic) == null) {
			topic = EventAdminAuditLoginModule.TOPIC_EVENTS;
		}else
			if (!(topic.endsWith("/"))) {
				topic += "/";
			}

	}

	static class EventAdminAuditor {
		public static void audit(BundleContext bundleContext, String topic, String username, Subject subject) {
			ServiceReference<EventAdmin> ref = bundleContext.getServiceReference(EventAdmin.class);
			if (ref != null) {
				EventAdmin eventAdmin = bundleContext.getService(ref);
				try {
					Map<String, Object> props = new HashMap<>();
					props.put("type", topic.substring(((topic.lastIndexOf("/")) + 1)).toLowerCase());
					props.put("timestamp", System.currentTimeMillis());
					props.put("username", username);
					props.put("subject", subject);
					eventAdmin.postEvent(new Event(topic, props));
				} finally {
					bundleContext.ungetService(ref);
				}
			}
		}
	}
}

