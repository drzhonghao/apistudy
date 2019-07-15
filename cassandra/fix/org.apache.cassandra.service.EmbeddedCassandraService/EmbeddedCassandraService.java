

import java.io.IOException;
import org.apache.cassandra.service.CassandraDaemon;


public class EmbeddedCassandraService {
	CassandraDaemon cassandraDaemon;

	public void start() throws IOException {
		cassandraDaemon.applyConfig();
		cassandraDaemon.init(null);
		cassandraDaemon.start();
	}
}

