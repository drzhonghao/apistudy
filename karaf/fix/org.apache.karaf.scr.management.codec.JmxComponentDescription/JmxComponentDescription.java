

import java.util.Arrays;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;


public class JmxComponentDescription {
	public static final CompositeType COMPONENT = JmxComponentDescription.createComponentType();

	public static final TabularType COMPONENT_TABLE = JmxComponentDescription.createComponentTableType();

	private final CompositeData data;

	public JmxComponentDescription(ComponentDescriptionDTO component) {
		data = null;
	}

	public CompositeData asCompositeData() {
		return data;
	}

	public static TabularData tableFrom(ComponentDescriptionDTO... components) {
		return JmxComponentDescription.tableFrom(Arrays.asList(components));
	}

	public static TabularData tableFrom(Iterable<ComponentDescriptionDTO> components) {
		TabularDataSupport table = new TabularDataSupport(JmxComponentDescription.COMPONENT_TABLE);
		for (ComponentDescriptionDTO component : components) {
			table.put(new JmxComponentDescription(component).asCompositeData());
		}
		return table;
	}

	private static CompositeType createComponentType() {
		String description = "This type encapsulates Scr references";
		return null;
	}

	private static TabularType createComponentTableType() {
		return null;
	}
}

