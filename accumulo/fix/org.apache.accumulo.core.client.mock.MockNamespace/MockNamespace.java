

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.accumulo.core.client.mock.MockAccumulo;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.security.NamespacePermission;


@Deprecated
public class MockNamespace {
	final HashMap<String, String> settings;

	Map<String, EnumSet<NamespacePermission>> userPermissions = new HashMap<>();

	public MockNamespace() {
		settings = new HashMap<>();
		for (Map.Entry<String, String> entry : AccumuloConfiguration.getDefaultConfiguration()) {
			String key = entry.getKey();
			if (key.startsWith(Property.TABLE_PREFIX.getKey())) {
				settings.put(key, entry.getValue());
			}
		}
	}

	public List<String> getTables(MockAccumulo acu) {
		List<String> l = new LinkedList<>();
		return l;
	}
}

