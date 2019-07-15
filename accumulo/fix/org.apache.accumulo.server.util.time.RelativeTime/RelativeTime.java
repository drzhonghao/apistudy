

import org.apache.accumulo.server.util.time.BaseRelativeTime;


public class RelativeTime extends BaseRelativeTime {
	private static BaseRelativeTime instance = new RelativeTime();

	public static BaseRelativeTime getInstance() {
		return RelativeTime.instance;
	}

	public static long currentTimeMillis() {
		return RelativeTime.getInstance().currentTime();
	}
}

