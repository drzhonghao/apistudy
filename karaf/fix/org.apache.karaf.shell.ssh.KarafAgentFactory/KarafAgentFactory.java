

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.shell.api.console.Session;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.agent.SshAgentServer;
import org.apache.sshd.agent.common.AgentDelegate;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.AgentServerProxy;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.PropertyResolver;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.session.ConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KarafAgentFactory implements SshAgentFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(KarafAgentFactory.class);

	private final Map<String, AgentServerProxy> proxies = new ConcurrentHashMap<>();

	private final Map<String, SshAgent> locals = new ConcurrentHashMap<>();

	private static final KarafAgentFactory INSTANCE = new KarafAgentFactory();

	public static KarafAgentFactory getInstance() {
		return KarafAgentFactory.INSTANCE;
	}

	@Override
	public List<NamedFactory<Channel>> getChannelForwardingFactories(FactoryManager factoryManager) {
		return LocalAgentFactory.DEFAULT_FORWARDING_CHANNELS;
	}

	public SshAgent createClient(FactoryManager manager) throws IOException {
		String proxyId = ((String) (manager.getProperties().get(SshAgent.SSH_AUTHSOCKET_ENV_NAME)));
		if (proxyId == null) {
			throw new IllegalStateException((("No " + (SshAgent.SSH_AUTHSOCKET_ENV_NAME)) + " environment variable set"));
		}
		AgentServerProxy proxy = proxies.get(proxyId);
		if (proxy != null) {
			return proxy.createClient();
		}
		SshAgent agent = locals.get(proxyId);
		if (agent != null) {
			return new AgentDelegate(agent);
		}
		throw new IllegalStateException("No ssh agent found");
	}

	public SshAgentServer createServer(ConnectionService service) throws IOException {
		final AgentServerProxy proxy = new AgentServerProxy(service);
		proxies.put(proxy.getId(), proxy);
		return new SshAgentServer() {
			public String getId() {
				return proxy.getId();
			}

			@Override
			public boolean isOpen() {
				return proxy.isOpen();
			}

			public void close() throws IOException {
				proxies.remove(proxy.getId());
				proxy.close();
			}
		};
	}

	public void registerSession(Session session) {
		try {
			String user = ((String) (session.get("USER")));
			SshAgent agent = new AgentImpl();
			URL url = getClass().getClassLoader().getResource("karaf.key");
			InputStream is = url.openStream();
			ObjectInputStream r = new ObjectInputStream(is);
			KeyPair keyPair = ((KeyPair) (r.readObject()));
			agent.addIdentity(keyPair, "karaf");
			String agentId = "local:" + user;
			session.put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, agentId);
			locals.put(agentId, agent);
		} catch (Throwable e) {
			KarafAgentFactory.LOGGER.warn("Error starting ssh agent for local console", e);
		}
	}

	public void unregisterSession(Session session) {
		try {
			if ((session != null) && ((session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME)) != null)) {
				String agentId = ((String) (session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME)));
				session.put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, null);
				if (agentId != null) {
					locals.remove(agentId);
				}
			}
		} catch (Throwable e) {
			KarafAgentFactory.LOGGER.warn("Error stopping ssh agent for local console", e);
		}
	}
}

