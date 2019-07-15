

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;


public class JdbcMBeanImpl {
	public TabularData query(String datasource, String query) throws MBeanException {
		try {
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
		return null;
	}
}

