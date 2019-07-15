

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.main.lock.DefaultJDBCLock;
import org.apache.karaf.main.lock.Statements;


public class OracleJDBCLock extends DefaultJDBCLock {
	private static final String MOMENT_COLUMN_DATA_TYPE = "NUMBER(20)";

	public OracleJDBCLock(Properties props) {
		super(props);
	}

	Statements createStatements() {
		Statements statements = new Statements();
		statements.setMomentColumnDataType(OracleJDBCLock.MOMENT_COLUMN_DATA_TYPE);
		return statements;
	}

	@Override
	public boolean lock() {
		return acquireLock();
	}

	boolean updateLock() {
		return acquireLock();
	}

	boolean acquireLock() {
		return false;
	}

	private boolean lockAcquiredOnNonEmptySelection() {
		PreparedStatement preparedStatement = null;
		boolean lockAcquired = false;
		try {
			ResultSet rs = preparedStatement.executeQuery();
			if (rs.next()) {
				lockAcquired = (rs.getInt(1)) > 0;
			}else {
			}
		} catch (Exception e) {
		} finally {
		}
		return lockAcquired;
	}
}

