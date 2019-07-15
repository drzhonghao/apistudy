

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.gms.ApplicationState;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.FailureDetectorMBean;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.gms.IFailureDetectionEventListener;
import org.apache.cassandra.gms.IFailureDetector;
import org.apache.cassandra.gms.VersionedValue;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.Clock;
import org.apache.cassandra.utils.FBUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FailureDetector implements FailureDetectorMBean , IFailureDetector {
	private static final Logger logger = LoggerFactory.getLogger(FailureDetector.class);

	public static final String MBEAN_NAME = "org.apache.cassandra.net:type=FailureDetector";

	private static final int SAMPLE_SIZE = 1000;

	protected static final long INITIAL_VALUE_NANOS = TimeUnit.NANOSECONDS.convert(FailureDetector.getInitialValue(), TimeUnit.MILLISECONDS);

	private static final int DEBUG_PERCENTAGE = 80;

	private static final long DEFAULT_MAX_PAUSE = 5000L * 1000000L;

	private static final long MAX_LOCAL_PAUSE_IN_NANOS = FailureDetector.getMaxLocalPause();

	private long lastInterpret = Clock.instance.nanoTime();

	private long lastPause = 0L;

	private static long getMaxLocalPause() {
		if ((System.getProperty("cassandra.max_local_pause_in_ms")) != null) {
			long pause = Long.parseLong(System.getProperty("cassandra.max_local_pause_in_ms"));
			FailureDetector.logger.warn("Overriding max local pause time to {}ms", pause);
			return pause * 1000000L;
		}else
			return FailureDetector.DEFAULT_MAX_PAUSE;

	}

	public static final IFailureDetector instance = new FailureDetector();

	private final double PHI_FACTOR = 1.0 / (Math.log(10.0));

	private final ConcurrentHashMap<InetAddress, ArrivalWindow> arrivalSamples = new ConcurrentHashMap<>();

	private final List<IFailureDetectionEventListener> fdEvntListeners = new CopyOnWriteArrayList<>();

	public FailureDetector() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.registerMBean(this, new ObjectName(FailureDetector.MBEAN_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static long getInitialValue() {
		String newvalue = System.getProperty("cassandra.fd_initial_value_ms");
		if (newvalue == null) {
			return (Gossiper.intervalInMillis) * 2;
		}else {
			FailureDetector.logger.info("Overriding FD INITIAL_VALUE to {}ms", newvalue);
			return Integer.parseInt(newvalue);
		}
	}

	public String getAllEndpointStates() {
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}

	public Map<String, String> getSimpleStates() {
		return null;
	}

	public int getDownEndpointCount() {
		int count = 0;
		return count;
	}

	public int getUpEndpointCount() {
		int count = 0;
		return count;
	}

	@Override
	public TabularData getPhiValues() throws OpenDataException {
		final CompositeType ct = new CompositeType("Node", "Node", new String[]{ "Endpoint", "PHI" }, new String[]{ "IP of the endpoint", "PHI value" }, new OpenType[]{ SimpleType.STRING, SimpleType.DOUBLE });
		final TabularDataSupport results = new TabularDataSupport(new TabularType("PhiList", "PhiList", ct, new String[]{ "Endpoint" }));
		for (final Map.Entry<InetAddress, ArrivalWindow> entry : arrivalSamples.entrySet()) {
			final ArrivalWindow window = entry.getValue();
			if ((window.mean()) > 0) {
				final double phi = window.getLastReportedPhi();
				if (phi != (Double.MIN_VALUE)) {
					final CompositeData data = new CompositeDataSupport(ct, new String[]{ "Endpoint", "PHI" }, new Object[]{ entry.getKey().toString(), phi * (PHI_FACTOR) });
					results.put(data);
				}
			}
		}
		return results;
	}

	public String getEndpointState(String address) throws UnknownHostException {
		StringBuilder sb = new StringBuilder();
		EndpointState endpointState = Gossiper.instance.getEndpointStateForEndpoint(InetAddress.getByName(address));
		appendEndpointState(sb, endpointState);
		return sb.toString();
	}

	private void appendEndpointState(StringBuilder sb, EndpointState endpointState) {
		for (Map.Entry<ApplicationState, VersionedValue> state : endpointState.states()) {
			if ((state.getKey()) == (ApplicationState.TOKENS))
				continue;

			sb.append("  ").append(state.getKey()).append(":").append(state.getValue().version).append(":").append(state.getValue().value).append("\n");
		}
		VersionedValue tokens = endpointState.getApplicationState(ApplicationState.TOKENS);
		if (tokens != null) {
			sb.append("  TOKENS:").append(tokens.version).append(":<hidden>\n");
		}else {
			sb.append("  TOKENS: not present\n");
		}
	}

	public void dumpInterArrivalTimes() {
		File file = FileUtils.createTempFile("failuredetector-", ".dat");
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file, true))) {
			os.write(toString().getBytes());
		} catch (IOException e) {
			throw new FSWriteError(e, file);
		}
	}

	public void setPhiConvictThreshold(double phi) {
		DatabaseDescriptor.setPhiConvictThreshold(phi);
	}

	public double getPhiConvictThreshold() {
		return DatabaseDescriptor.getPhiConvictThreshold();
	}

	public boolean isAlive(InetAddress ep) {
		if (ep.equals(FBUtilities.getBroadcastAddress()))
			return true;

		EndpointState epState = Gossiper.instance.getEndpointStateForEndpoint(ep);
		if (epState == null)
			FailureDetector.logger.error(("Unknown endpoint: " + ep), new IllegalArgumentException(""));

		return (epState != null) && (epState.isAlive());
	}

	public void report(InetAddress ep) {
		long now = Clock.instance.nanoTime();
		ArrivalWindow heartbeatWindow = arrivalSamples.get(ep);
		if (heartbeatWindow == null) {
			heartbeatWindow = new ArrivalWindow(FailureDetector.SAMPLE_SIZE);
			heartbeatWindow.add(now, ep);
			heartbeatWindow = arrivalSamples.putIfAbsent(ep, heartbeatWindow);
			if (heartbeatWindow != null)
				heartbeatWindow.add(now, ep);

		}else {
			heartbeatWindow.add(now, ep);
		}
		if ((FailureDetector.logger.isTraceEnabled()) && (heartbeatWindow != null))
			FailureDetector.logger.trace("Average for {} is {}", ep, heartbeatWindow.mean());

	}

	public void interpret(InetAddress ep) {
		ArrivalWindow hbWnd = arrivalSamples.get(ep);
		if (hbWnd == null) {
			return;
		}
		long now = Clock.instance.nanoTime();
		long diff = now - (lastInterpret);
		lastInterpret = now;
		if (diff > (FailureDetector.MAX_LOCAL_PAUSE_IN_NANOS)) {
			FailureDetector.logger.warn("Not marking nodes down due to local pause of {} > {}", diff, FailureDetector.MAX_LOCAL_PAUSE_IN_NANOS);
			lastPause = now;
			return;
		}
		if (((Clock.instance.nanoTime()) - (lastPause)) < (FailureDetector.MAX_LOCAL_PAUSE_IN_NANOS)) {
			FailureDetector.logger.debug("Still not marking nodes down due to local pause");
			return;
		}
		double phi = hbWnd.phi(now);
		if (FailureDetector.logger.isTraceEnabled())
			FailureDetector.logger.trace("PHI for {} : {}", ep, phi);

		if (((PHI_FACTOR) * phi) > (getPhiConvictThreshold())) {
			if (FailureDetector.logger.isTraceEnabled())
				FailureDetector.logger.trace("Node {} phi {} > {}; intervals: {} mean: {}", new Object[]{ ep, (PHI_FACTOR) * phi, getPhiConvictThreshold(), hbWnd, hbWnd.mean() });

			for (IFailureDetectionEventListener listener : fdEvntListeners) {
				listener.convict(ep, phi);
			}
		}else
			if ((FailureDetector.logger.isDebugEnabled()) && (((((PHI_FACTOR) * phi) * (FailureDetector.DEBUG_PERCENTAGE)) / 100.0) > (getPhiConvictThreshold()))) {
				FailureDetector.logger.debug("PHI for {} : {}", ep, phi);
			}else
				if (FailureDetector.logger.isTraceEnabled()) {
					FailureDetector.logger.trace("PHI for {} : {}", ep, phi);
					FailureDetector.logger.trace("mean for {} : {}", ep, hbWnd.mean());
				}


	}

	public void forceConviction(InetAddress ep) {
		FailureDetector.logger.debug("Forcing conviction of {}", ep);
		for (IFailureDetectionEventListener listener : fdEvntListeners) {
			listener.convict(ep, getPhiConvictThreshold());
		}
	}

	public void remove(InetAddress ep) {
		arrivalSamples.remove(ep);
	}

	public void registerFailureDetectionEventListener(IFailureDetectionEventListener listener) {
		fdEvntListeners.add(listener);
	}

	public void unregisterFailureDetectionEventListener(IFailureDetectionEventListener listener) {
		fdEvntListeners.remove(listener);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		Set<InetAddress> eps = arrivalSamples.keySet();
		sb.append("-----------------------------------------------------------------------");
		for (InetAddress ep : eps) {
			ArrivalWindow hWnd = arrivalSamples.get(ep);
			sb.append(ep).append(" : ");
			sb.append(hWnd);
			sb.append(System.getProperty("line.separator"));
		}
		sb.append("-----------------------------------------------------------------------");
		return sb.toString();
	}
}

