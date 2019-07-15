

import java.sql.Connection;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.main.lock.DefaultJDBCLock;
import org.apache.karaf.main.lock.Statements;


public class MySQLJDBCLock extends DefaultJDBCLock {
	public MySQLJDBCLock(Properties props) {
		super(props);
	}

	Statements createStatements() {
		Statements statements = new Statements();
		return statements;
	}

	Connection createConnection(String driver, String url, String username, String password) throws Exception {
		url = (url.toLowerCase().contains("createDatabaseIfNotExist=true")) ? url : url.contains("?") ? url + "&createDatabaseIfNotExist=true" : url + "?createDatabaseIfNotExist=true";
		return null;
	}
}

