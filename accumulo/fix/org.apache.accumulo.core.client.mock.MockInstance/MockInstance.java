

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.mock.MockAccumulo;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.util.ByteBufferUtil;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.Text;


@Deprecated
public class MockInstance implements Instance {
	static final String genericAddress = "localhost:1234";

	static final Map<String, MockAccumulo> instances = new HashMap<>();

	MockAccumulo acu;

	String instanceName;

	public MockInstance() {
		instanceName = "mock-instance";
	}

	static FileSystem getDefaultFileSystem() {
		try {
			Configuration conf = CachedConfiguration.getInstance();
			conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
			conf.set("fs.default.name", "file:///");
			return FileSystem.get(CachedConfiguration.getInstance());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public MockInstance(String instanceName) {
		this(instanceName, MockInstance.getDefaultFileSystem());
	}

	public MockInstance(String instanceName, FileSystem fs) {
		synchronized(MockInstance.instances) {
			if (MockInstance.instances.containsKey(instanceName))
				acu = MockInstance.instances.get(instanceName);
			else {
			}
		}
		this.instanceName = instanceName;
	}

	@Override
	public String getRootTabletLocation() {
		return MockInstance.genericAddress;
	}

	@Override
	public List<String> getMasterLocations() {
		return Collections.singletonList(MockInstance.genericAddress);
	}

	@Override
	public String getInstanceID() {
		return "mock-instance-id";
	}

	@Override
	public String getInstanceName() {
		return instanceName;
	}

	@Override
	public String getZooKeepers() {
		return "localhost";
	}

	@Override
	public int getZooKeepersSessionTimeOut() {
		return 30 * 1000;
	}

	@Override
	@Deprecated
	public Connector getConnector(String user, byte[] pass) throws AccumuloException, AccumuloSecurityException {
		return getConnector(user, new PasswordToken(pass));
	}

	@Override
	@Deprecated
	public Connector getConnector(String user, ByteBuffer pass) throws AccumuloException, AccumuloSecurityException {
		return getConnector(user, ByteBufferUtil.toBytes(pass));
	}

	@Override
	@Deprecated
	public Connector getConnector(String user, CharSequence pass) throws AccumuloException, AccumuloSecurityException {
		return getConnector(user, TextUtil.getBytes(new Text(pass.toString())));
	}

	AccumuloConfiguration conf = null;

	@Deprecated
	@Override
	public AccumuloConfiguration getConfiguration() {
		return (conf) == null ? DefaultConfiguration.getInstance() : conf;
	}

	@Override
	@Deprecated
	public void setConfiguration(AccumuloConfiguration conf) {
		this.conf = conf;
	}

	@Override
	public Connector getConnector(String principal, AuthenticationToken token) throws AccumuloException, AccumuloSecurityException {
		return null;
	}
}

