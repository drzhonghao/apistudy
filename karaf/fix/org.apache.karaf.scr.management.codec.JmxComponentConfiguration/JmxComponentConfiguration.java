

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;


public class JmxComponentConfiguration {
	public static final CompositeType COMPONENT_CONFIGURATION = JmxComponentConfiguration.createComponentConfigurationType();

	public static final TabularType COMPONENT_TABLE = JmxComponentConfiguration.createComponentTableType();

	private final CompositeData data;

	public JmxComponentConfiguration(ComponentConfigurationDTO component) {
		data = null;
	}

	private String getState(ComponentConfigurationDTO component) {
		switch (component.state) {
			case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION :
				return "Unsatisfied configuration";
			case ComponentConfigurationDTO.UNSATISFIED_REFERENCE :
				return "Unstatisfied reference";
			case ComponentConfigurationDTO.SATISFIED :
				return "Statisfied";
			case ComponentConfigurationDTO.ACTIVE :
				return "Active";
		}
		return "Unknown";
	}

	public CompositeData asCompositeData() {
		return data;
	}

	public static TabularData tableFrom(ComponentConfigurationDTO... components) {
		return JmxComponentConfiguration.tableFrom(Arrays.asList(components));
	}

	public static TabularData tableFrom(Collection<ComponentConfigurationDTO> components) {
		return JmxComponentConfiguration.tableFrom(components.stream());
	}

	public static TabularData tableFrom(Stream<ComponentConfigurationDTO> components) {
		return components.map(JmxComponentConfiguration::new).map(JmxComponentConfiguration::asCompositeData).collect(() -> new TabularDataSupport(JmxComponentConfiguration.COMPONENT_TABLE), TabularDataSupport::put, TabularDataSupport::putAll);
	}

	private static CompositeType createComponentConfigurationType() {
		String description = "This type encapsulates Scr references";
		return null;
	}

	private static TabularType createComponentTableType() {
		return null;
	}
}

