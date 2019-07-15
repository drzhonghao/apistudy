

import java.util.List;
import java.util.Map;
import javax.management.MBeanException;


public class JndiMBeanImpl {
	public Map<String, String> getNames() throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
		return null;
	}

	public List<String> getContexts() throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
		return null;
	}

	public void alias(String name, String alias) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
	}

	public void bind(Long serviceId, String name) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
	}

	public void unbind(String name) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
	}

	public void create(String context) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
	}

	public void delete(String context) throws MBeanException {
		try {
		} catch (Throwable t) {
			throw new MBeanException(null, t.getMessage());
		}
	}
}

