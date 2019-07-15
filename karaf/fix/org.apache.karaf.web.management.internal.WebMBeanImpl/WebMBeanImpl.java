

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;


public class WebMBeanImpl extends StandardMBean {
	public TabularData getWebBundles() throws MBeanException {
		try {
			CompositeType webType = new CompositeType("Web Bundle", "An OSGi Web bundle", new String[]{ "ID", "State", "Web-State", "Level", "Web-ContextPath", "Name" }, new String[]{ "ID of the bundle", "OSGi state of the bundle", "Web state of the bundle", "Start level of the bundle", "Web context path", "Name of the bundle" }, new OpenType[]{ SimpleType.LONG, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING });
			TabularType tableType = new TabularType("Web Bundles", "Table of web bundles", webType, new String[]{ "ID" });
			TabularData table = new TabularDataSupport(tableType);
			return table;
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}

	public void start(Long bundleId) throws MBeanException {
		try {
			List<Long> list = new ArrayList<>();
			list.add(bundleId);
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}

	public void start(List<Long> bundleIds) throws MBeanException {
		try {
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}

	public void stop(Long bundleId) throws MBeanException {
		try {
			List<Long> list = new ArrayList<>();
			list.add(bundleId);
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}

	public void stop(List<Long> bundleIds) throws MBeanException {
		try {
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}
}

