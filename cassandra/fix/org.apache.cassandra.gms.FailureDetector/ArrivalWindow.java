

import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ArrivalWindow {
	private static final Logger logger = LoggerFactory.getLogger(ArrivalWindow.class);

	private long tLast = 0L;

	private final ArrayBackedBoundedStats arrivalIntervals;

	private double lastReportedPhi = Double.MIN_VALUE;

	private final long MAX_INTERVAL_IN_NANO = ArrivalWindow.getMaxInterval();

	ArrivalWindow(int size) {
		arrivalIntervals = new ArrayBackedBoundedStats(size);
	}

	private static long getMaxInterval() {
		String newvalue = System.getProperty("cassandra.fd_max_interval_ms");
		if (newvalue == null) {
			return FailureDetector.INITIAL_VALUE_NANOS;
		}else {
			ArrivalWindow.logger.info("Overriding FD MAX_INTERVAL to {}ms", newvalue);
			return TimeUnit.NANOSECONDS.convert(Integer.parseInt(newvalue), TimeUnit.MILLISECONDS);
		}
	}

	synchronized void add(long value, InetAddress ep) {
		assert (tLast) >= 0;
		if ((tLast) > 0L) {
			long interArrivalTime = value - (tLast);
			if (interArrivalTime <= (MAX_INTERVAL_IN_NANO)) {
				arrivalIntervals.add(interArrivalTime);
				ArrivalWindow.logger.trace("Reporting interval time of {} for {}", interArrivalTime, ep);
			}else {
				ArrivalWindow.logger.trace("Ignoring interval time of {} for {}", interArrivalTime, ep);
			}
		}else {
			arrivalIntervals.add(FailureDetector.INITIAL_VALUE_NANOS);
		}
		tLast = value;
	}

	double mean() {
		return arrivalIntervals.mean();
	}

	double phi(long tnow) {
		assert ((arrivalIntervals.mean()) > 0) && ((tLast) > 0);
		long t = tnow - (tLast);
		lastReportedPhi = t / (mean());
		return lastReportedPhi;
	}

	double getLastReportedPhi() {
		return lastReportedPhi;
	}

	public String toString() {
		return Arrays.toString(arrivalIntervals.getArrivalIntervals());
	}
}

