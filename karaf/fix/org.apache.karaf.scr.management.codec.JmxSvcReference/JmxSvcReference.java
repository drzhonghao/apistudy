

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;


public class JmxSvcReference {
	public static final CompositeType SVC_REFERENCE = JmxSvcReference.createReferenceType();

	public static final TabularType SVC_REFERENCE_TABLE = JmxSvcReference.createReferenceTableType();

	private final CompositeData data;

	public JmxSvcReference(SatisfiedReferenceDTO reference) {
		data = null;
	}

	public JmxSvcReference(UnsatisfiedReferenceDTO reference) {
		data = null;
	}

	public CompositeData asCompositeData() {
		return data;
	}

	public static TabularData tableFrom(SatisfiedReferenceDTO[] references) {
		TabularDataSupport table = new TabularDataSupport(JmxSvcReference.SVC_REFERENCE_TABLE);
		if (references != null) {
			for (SatisfiedReferenceDTO reference : references) {
				table.put(new JmxSvcReference(reference).asCompositeData());
			}
		}
		return table;
	}

	public static TabularData tableFrom(UnsatisfiedReferenceDTO[] references) {
		TabularDataSupport table = new TabularDataSupport(JmxSvcReference.SVC_REFERENCE_TABLE);
		if (references != null) {
			for (UnsatisfiedReferenceDTO reference : references) {
				table.put(new JmxSvcReference(reference).asCompositeData());
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

	private static Long[] getBoundServices(ServiceReferenceDTO[] references) {
		if ((references == null) || ((references.length) == 0)) {
			return new Long[0];
		}else {
			Long[] ids = new Long[references.length];
			for (int i = 0; i < (references.length); i++) {
				ids[i] = references[i].id;
			}
			return ids;
		}
	}
}

