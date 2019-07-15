

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jdbc", name = "ds-create", description = "Create a JDBC datasource config for pax-jdbc-config from a DataSourceFactory")
@Service
public class CreateCommand {
	@Argument(index = 0, name = "name", description = "The JDBC datasource name", required = true, multiValued = false)
	String name;

	@Option(name = "-dn", aliases = { "--driverName" }, description = "org.osgi.driver.name property of the DataSourceFactory", required = false, multiValued = false)
	String driverName;

	@Option(name = "-dc", aliases = { "--driverClass" }, description = "org.osgi.driver.class property  of the DataSourceFactory", required = false, multiValued = false)
	String driverClass;

	@Option(name = "-dbName", description = "Database name to use", required = false, multiValued = false)
	String databaseName;

	@Option(name = "-url", description = "The JDBC URL to use", required = false, multiValued = false)
	String url;

	@Option(name = "-u", aliases = { "--username" }, description = "The database username", required = false, multiValued = false)
	String username;

	@Option(name = "-p", aliases = { "--password" }, description = "The database password", required = false, multiValued = false)
	String password;

	@Option(name = "-dt", aliases = { "--databaseType" }, description = "The database type (ConnectionPoolDataSource, XADataSource or DataSource)", required = false, multiValued = false)
	String databaseType;

	public Object execute() throws Exception {
		return null;
	}
}

