

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.main.lock.DefaultJDBCLock;
import org.apache.karaf.main.lock.Statements;


public class SQLServerJDBCLock extends DefaultJDBCLock {
	public SQLServerJDBCLock(Properties properties) {
		super(properties);
	}

	Statements createStatements() {
		Statements statements = new SQLServerJDBCLock.MsSQLServerStatements();
		return statements;
	}

	private class MsSQLServerStatements extends Statements {
		@Override
		public String getLockCreateStatement() {
			return "SELECT * FROM " + (getFullLockTableName());
		}
	}
}

