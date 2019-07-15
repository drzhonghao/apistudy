

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.accumulo.minicluster.ServerType;
import org.apache.accumulo.minicluster.impl.MiniAccumuloClusterImpl;
import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;


public class MiniAccumuloCluster {
	private MiniAccumuloClusterImpl impl;

	private MiniAccumuloCluster(MiniAccumuloConfigImpl config) throws IOException {
		impl = new MiniAccumuloClusterImpl(config);
	}

	public MiniAccumuloCluster(File dir, String rootPassword) throws IOException {
		this(new MiniAccumuloConfigImpl(dir, rootPassword));
	}

	public MiniAccumuloCluster(MiniAccumuloConfig config) throws IOException {
	}

	public void start() throws IOException, InterruptedException {
		impl.start();
	}

	public Set<Pair<ServerType, Integer>> getDebugPorts() {
		return impl.getDebugPorts();
	}

	public String getInstanceName() {
		return impl.getInstanceName();
	}

	public String getZooKeepers() {
		return impl.getZooKeepers();
	}

	public void stop() throws IOException, InterruptedException {
		impl.stop();
	}

	public MiniAccumuloConfig getConfig() {
		return null;
	}

	public Connector getConnector(String user, String passwd) throws AccumuloException, AccumuloSecurityException {
		return impl.getConnector(user, new PasswordToken(passwd));
	}

	public ClientConfiguration getClientConfig() {
		return impl.getClientConfig();
	}
}

