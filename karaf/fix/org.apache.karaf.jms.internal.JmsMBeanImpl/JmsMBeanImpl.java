

import java.util.List;
import java.util.Map;
import javax.management.MBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;


public class JmsMBeanImpl {
	public void delete(String name) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
	}

	public Map<String, String> info(String connectionFactory, String username, String password) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
		return null;
	}

	public int count(String connectionFactory, String queue, String username, String password) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
		return 0;
	}

	public List<String> queues(String connectionFactory, String username, String password) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
		return null;
	}

	public List<String> topics(String connectionFactory, String username, String password) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
		return null;
	}

	public void send(String connectionFactory, String queue, String content, String replyTo, String username, String password) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
	}

	public int consume(String connectionFactory, String queue, String selector, String username, String password) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
		return 0;
	}

	public int move(String connectionFactory, String source, String destination, String selector, String username, String password) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
		return 0;
	}

	public TabularData browse(String connectionFactory, String queue, String selector, String username, String password) throws MBeanException {
		try {
			CompositeType type = new CompositeType("message", "JMS Message", new String[]{ "id", "content", "charset", "type", "correlation", "delivery", "destination", "expiration", "priority", "redelivered", "replyto", "timestamp" }, new String[]{ "Message ID", "Content", "Charset", "Type", "Correlation ID", "Delivery Mode", "Destination", "Expiration Date", "Priority", "Redelivered", "Reply-To", "Timestamp" }, new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.BOOLEAN, SimpleType.STRING, SimpleType.STRING });
			TabularType tableType = new TabularType("messages", "JMS Messages", type, new String[]{ "id" });
			TabularData table = new TabularDataSupport(tableType);
			return table;
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
	}
}

