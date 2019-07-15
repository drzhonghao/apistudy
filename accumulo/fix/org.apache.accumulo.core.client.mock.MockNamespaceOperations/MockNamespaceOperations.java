

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.NamespaceExistsException;
import org.apache.accumulo.core.client.NamespaceNotEmptyException;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.impl.NamespaceOperationsHelper;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.mock.MockAccumulo;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Deprecated
class MockNamespaceOperations extends NamespaceOperationsHelper {
	private static final Logger log = LoggerFactory.getLogger(MockNamespaceOperations.class);

	private final MockAccumulo acu;

	private final String username;

	MockNamespaceOperations(MockAccumulo acu, String username) {
		this.acu = acu;
		this.username = username;
	}

	@Override
	public SortedSet<String> list() {
		return null;
	}

	@Override
	public boolean exists(String namespace) {
		return false;
	}

	@Override
	public void create(String namespace) throws AccumuloException, AccumuloSecurityException, NamespaceExistsException {
		if (!(namespace.matches(Namespaces.VALID_NAME_REGEX)))
			throw new IllegalArgumentException();

		if (exists(namespace))
			throw new NamespaceExistsException(namespace, namespace, "");
		else
			acu.createNamespace(username, namespace);

	}

	@Override
	public void delete(String namespace) throws AccumuloException, AccumuloSecurityException, NamespaceNotEmptyException, NamespaceNotFoundException {
	}

	@Override
	public void rename(String oldNamespaceName, String newNamespaceName) throws AccumuloException, AccumuloSecurityException, NamespaceExistsException, NamespaceNotFoundException {
		if (!(exists(oldNamespaceName)))
			throw new NamespaceNotFoundException(oldNamespaceName, oldNamespaceName, "");

		if (exists(newNamespaceName))
			throw new NamespaceExistsException(newNamespaceName, newNamespaceName, "");

	}

	@Override
	public void setProperty(String namespace, String property, String value) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public void removeProperty(String namespace, String property) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public Iterable<Map.Entry<String, String>> getProperties(String namespace) throws NamespaceNotFoundException {
		if (!(exists(namespace))) {
			throw new NamespaceNotFoundException(namespace, namespace, "");
		}
		return null;
	}

	@Override
	public Map<String, String> namespaceIdMap() {
		Map<String, String> result = new HashMap<>();
		return result;
	}

	@Override
	public boolean testClassLoad(String namespace, String className, String asTypeName) throws AccumuloException, AccumuloSecurityException, NamespaceNotFoundException {
		try {
			AccumuloVFSClassLoader.loadClass(className, Class.forName(asTypeName));
		} catch (ClassNotFoundException e) {
			MockNamespaceOperations.log.warn((((("Could not load class '" + className) + "' with type name '") + asTypeName) + "' in testClassLoad()"), e);
			return false;
		}
		return true;
	}
}

