

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.net.CallbackInfo;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.tracing.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ResponseVerbHandler implements IVerbHandler {
	private static final Logger logger = LoggerFactory.getLogger(ResponseVerbHandler.class);

	public void doVerb(MessageIn message, int id) {
		long latency = TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - (MessagingService.instance().getRegisteredCallbackAge(id))));
		CallbackInfo callbackInfo = MessagingService.instance().removeRegisteredCallback(id);
		if (callbackInfo == null) {
			String msg = "Callback already removed for {} (from {})";
			ResponseVerbHandler.logger.trace(msg, id, message.from);
			Tracing.trace(msg, id, message.from);
			return;
		}
		Tracing.trace("Processing response from {}", message.from);
		if (message.isFailureResponse()) {
		}else {
		}
	}
}

