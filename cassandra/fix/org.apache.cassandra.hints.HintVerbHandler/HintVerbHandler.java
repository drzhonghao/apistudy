

import java.net.InetAddress;
import org.apache.cassandra.hints.HintMessage;
import org.apache.cassandra.net.IVerbHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class HintVerbHandler implements IVerbHandler<HintMessage> {
	private static final Logger logger = LoggerFactory.getLogger(HintVerbHandler.class);

	private static void reply(int id, InetAddress to) {
	}
}

