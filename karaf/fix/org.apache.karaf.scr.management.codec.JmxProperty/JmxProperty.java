

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;


public class JmxProperty {
	public static final CompositeType PROPERTY = JmxProperty.createPropertyType();

	public static final TabularType PROPERTY_TABLE = JmxProperty.createPropertyTableType();

	private final CompositeData data;

	public JmxProperty(String key, String value) {
		data = null;
	}

	public CompositeData asCompositeData() {
		return data;
	}

	public static TabularData tableFrom(Map<String, Object> properties) {
		TabularDataSupport table = new TabularDataSupport(JmxProperty.PROPERTY_TABLE);
		for (Map.Entry<String, Object> e : properties.entrySet()) {
			String key = e.getKey();
			Object value = e.getValue();
			table.put(new JmxProperty(key, String.valueOf(value)).asCompositeData());
		}
		return table;
	}

	private static CompositeType createPropertyType() {
		String description = "This type encapsulates Scr properties";
		return null;
	}

	private static TabularType createPropertyTableType() {
		return null;
	}
}

