

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyServiceImpl {
	private static Logger LOG = LoggerFactory.getLogger(ProxyServiceImpl.class.getName());

	protected static final String CONFIGURATION_PID = "org.apache.karaf.http";

	protected static final String CONFIGURATION_KEY = "proxies";

	private ConfigurationAdmin configurationAdmin;

	private HttpService httpService;

	private Map<String, String> proxies;

	public ProxyServiceImpl(HttpService httpService, ConfigurationAdmin configurationAdmin) {
		this.httpService = httpService;
		this.configurationAdmin = configurationAdmin;
		this.proxies = new HashMap<>();
		initProxies();
	}

	public Map<String, String> getProxies() {
		return proxies;
	}

	public void addProxy(String url, String proxyTo) throws Exception {
		addProxyInternal(url, proxyTo);
		updateConfiguration();
	}

	public void removeProxy(String url) throws Exception {
		ProxyServiceImpl.LOG.debug(("removing proxy alias: " + url));
		httpService.unregister(url);
		proxies.remove(url);
		updateConfiguration();
	}

	public void initProxies() {
		ProxyServiceImpl.LOG.debug("unregistering and registering all configured proxies");
		unregisterAllProxies();
		initProxiesInternal();
	}

	private void initProxiesInternal() {
		try {
			Configuration configuration = getConfiguration();
			Dictionary<String, Object> configurationProperties = configuration.getProperties();
			String[] proxiesArray = getConfiguredProxyArray(configurationProperties);
			if (proxiesArray != null) {
				for (String proxyPair : proxiesArray) {
					String[] split = proxyPair.split(" ", 2);
					if ((split.length) == 2) {
						String from = split[0].trim();
						String to = split[1].trim();
						if (((from.length()) > 0) && ((to.length()) > 0)) {
							addProxyInternal(from, to);
						}
					}
				}
			}
		} catch (Exception e) {
			ProxyServiceImpl.LOG.error(("unable to initialize proxies: " + (e.getMessage())));
		}
	}

	private void addProxyInternal(String url, String proxyTo) {
		ProxyServiceImpl.LOG.debug(((("adding proxy alias: " + url) + ", proxied to: ") + proxyTo));
		try {
			proxies.put(url, proxyTo);
		} catch (Exception e) {
			ProxyServiceImpl.LOG.error(((((("could not add proxy alias: " + url) + ", proxied to: ") + proxyTo) + ", reason: ") + (e.getMessage())));
		}
	}

	private void updateConfiguration() {
		try {
			Configuration configuration = getConfiguration();
			Dictionary<String, Object> configurationProperties = configuration.getProperties();
			if (configurationProperties == null) {
				configurationProperties = new Hashtable<String, Object>();
			}
			configurationProperties.put(ProxyServiceImpl.CONFIGURATION_KEY, mapToProxyArray(this.proxies));
			configuration.update(configurationProperties);
		} catch (Exception e) {
			ProxyServiceImpl.LOG.error(("unable to update http proxy from configuration: " + (e.getMessage())));
		}
	}

	private Configuration getConfiguration() {
		try {
			return configurationAdmin.getConfiguration(ProxyServiceImpl.CONFIGURATION_PID, null);
		} catch (IOException e) {
			throw new RuntimeException("Error retrieving http proxy information from config admin", e);
		}
	}

	private String[] mapToProxyArray(Map<String, String> proxies) {
		List<String> proxyList = new ArrayList<String>();
		Iterator<Map.Entry<String, String>> entries = proxies.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<String, String> entry = entries.next();
			proxyList.add((((entry.getKey()) + " ") + (entry.getValue())));
		} 
		return proxyList.stream().toArray(String[]::new);
	}

	private String[] getConfiguredProxyArray(Dictionary<String, Object> configurationProperties) {
		Object val = null;
		if (configurationProperties != null) {
			val = configurationProperties.get(ProxyServiceImpl.CONFIGURATION_KEY);
		}
		if (val instanceof String[]) {
			return ((String[]) (val));
		}else {
			return null;
		}
	}

	private void unregisterAllProxies() {
		for (String url : proxies.keySet()) {
			ProxyServiceImpl.LOG.debug(("removing proxy alias: " + url));
			httpService.unregister(url);
		}
		proxies.clear();
	}
}

