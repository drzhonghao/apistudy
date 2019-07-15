

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.main.lock.DefaultJDBCLock;


public class PostgreSQLJDBCLock extends DefaultJDBCLock {
	public PostgreSQLJDBCLock(Properties props) {
		super(props);
	}

	boolean acquireLock() {
		return false;
	}
}

