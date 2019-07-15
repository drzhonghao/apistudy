

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.osgi.service.component.runtime.dto.ReferenceDTO;


public class JmxReference {
	public static final CompositeType REFERENCE = JmxReference.createReferenceType();

	public static final TabularType REFERENCE_TABLE = JmxReference.createReferenceTableType();

	private final CompositeData data;

	public JmxReference(ReferenceDTO reference) {
		data = null;
	}

	public CompositeData asCompositeData() {
		return data;
	}

	public static TabularData tableFrom(ReferenceDTO[] references) {
		TabularDataSupport table = new TabularDataSupport(JmxReference.REFERENCE_TABLE);
		if (references != null) {
			for (ReferenceDTO reference : references) {
				table.put(new JmxReference(reference).asCompositeData());
			}
		}
		return table;
	}

	private static CompositeType createReferenceType() {
		String description = "This type encapsulates Scr references";
		return null;
	}

	private static TabularType createReferenceTableType() {
		return null;
	}
}

