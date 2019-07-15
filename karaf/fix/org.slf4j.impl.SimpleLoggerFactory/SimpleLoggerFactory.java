

import java.util.HashMap;
import java.util.Map;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;


public class SimpleLoggerFactory implements ILoggerFactory {
	static final SimpleLoggerFactory INSTANCE = new SimpleLoggerFactory();

	Map<String, Logger> loggerMap;

	public SimpleLoggerFactory() {
		loggerMap = new HashMap<>();
	}

	public Logger getLogger(String name) {
		Logger slogger = null;
		synchronized(this) {
			slogger = loggerMap.get(name);
			if (slogger == null) {
				loggerMap.put(name, slogger);
			}
		}
		return slogger;
	}
}

