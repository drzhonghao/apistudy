

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.admin.ActiveCompaction;
import org.apache.accumulo.core.client.admin.ActiveScan;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.mock.MockAccumulo;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Deprecated
class MockInstanceOperations implements InstanceOperations {
	private static final Logger log = LoggerFactory.getLogger(MockInstanceOperations.class);

	MockAccumulo acu;

	public MockInstanceOperations(MockAccumulo acu) {
		this.acu = acu;
	}

	@Override
	public void setProperty(String property, String value) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public void removeProperty(String property) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public Map<String, String> getSystemConfiguration() throws AccumuloException, AccumuloSecurityException {
		return null;
	}

	@Override
	public Map<String, String> getSiteConfiguration() throws AccumuloException, AccumuloSecurityException {
		return null;
	}

	@Override
	public List<String> getTabletServers() {
		return new ArrayList<>();
	}

	@Override
	public List<ActiveScan> getActiveScans(String tserver) throws AccumuloException, AccumuloSecurityException {
		return new ArrayList<>();
	}

	@Override
	public boolean testClassLoad(String className, String asTypeName) throws AccumuloException, AccumuloSecurityException {
		try {
			AccumuloVFSClassLoader.loadClass(className, Class.forName(asTypeName));
		} catch (ClassNotFoundException e) {
			MockInstanceOperations.log.warn((("Could not find class named '" + className) + "' in testClassLoad."), e);
			return false;
		}
		return true;
	}

	@Override
	public List<ActiveCompaction> getActiveCompactions(String tserver) throws AccumuloException, AccumuloSecurityException {
		return new ArrayList<>();
	}

	@Override
	public void ping(String tserver) throws AccumuloException {
	}

	@Override
	public void waitForBalance() throws AccumuloException {
	}
}

