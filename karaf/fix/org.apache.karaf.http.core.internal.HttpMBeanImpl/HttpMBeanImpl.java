

import java.util.Map;
import javax.management.MBeanException;
import javax.management.StandardMBean;


public class HttpMBeanImpl extends StandardMBean {
	public Map<String, String> getProxies() throws MBeanException {
		return null;
	}

	public void addProxy(String url, String proxyTo) throws MBeanException {
		try {
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}
}

