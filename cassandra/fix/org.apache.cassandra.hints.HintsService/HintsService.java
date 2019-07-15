

import com.codahale.metrics.Counter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.gms.FailureDetector;
import org.apache.cassandra.gms.IFailureDetector;
import org.apache.cassandra.hints.Hint;
import org.apache.cassandra.hints.HintsServiceMBean;
import org.apache.cassandra.metrics.HintedHandoffMetrics;
import org.apache.cassandra.metrics.StorageMetrics;
import org.apache.cassandra.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class HintsService implements HintsServiceMBean {
	private static final Logger logger = LoggerFactory.getLogger(HintsService.class);

	public static HintsService instance = new HintsService();

	private static final String MBEAN_NAME = "org.apache.cassandra.hints:type=HintsService";

	private static final int MIN_BUFFER_SIZE = 32 << 20;

	static final ImmutableMap<String, Object> EMPTY_PARAMS = ImmutableMap.of();

	private final AtomicBoolean isDispatchPaused;

	private volatile boolean isShutDown = false;

	private final ScheduledFuture triggerFlushingFuture;

	private volatile ScheduledFuture triggerDispatchFuture;

	public final HintedHandoffMetrics metrics;

	private HintsService() {
		this(FailureDetector.instance);
	}

	@VisibleForTesting
	HintsService(IFailureDetector failureDetector) {
		File hintsDirectory = DatabaseDescriptor.getHintsDirectory();
		int maxDeliveryThreads = DatabaseDescriptor.getMaxHintsDeliveryThreads();
		int bufferSize = Math.max(((DatabaseDescriptor.getMaxMutationSize()) * 2), HintsService.MIN_BUFFER_SIZE);
		isDispatchPaused = new AtomicBoolean(true);
		int flushPeriod = DatabaseDescriptor.getHintsFlushPeriodInMS();
		metrics = new HintedHandoffMetrics();
		triggerFlushingFuture = null;
	}

	private static ImmutableMap<String, Object> createDescriptorParams() {
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		ParameterizedClass compressionConfig = DatabaseDescriptor.getHintsCompression();
		if (compressionConfig != null) {
			ImmutableMap.Builder<String, Object> compressorParams = ImmutableMap.builder();
			compressorParams.put(ParameterizedClass.CLASS_NAME, compressionConfig.class_name);
			if ((compressionConfig.parameters) != null) {
				compressorParams.put(ParameterizedClass.PARAMETERS, compressionConfig.parameters);
			}
		}
		return builder.build();
	}

	public void registerMBean() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean(this, new ObjectName(HintsService.MBEAN_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void write(Iterable<UUID> hostIds, Hint hint) {
		if (isShutDown)
			throw new IllegalStateException("HintsService is shut down and can't accept new hints");

		StorageMetrics.totalHints.inc(Iterables.size(hostIds));
	}

	public void write(UUID hostId, Hint hint) {
		write(Collections.singleton(hostId), hint);
	}

	void writeForAllReplicas(Hint hint) {
	}

	public void flushAndFsyncBlockingly(Iterable<UUID> hostIds) {
	}

	public synchronized void startDispatch() {
		if (isShutDown)
			throw new IllegalStateException("HintsService is shut down and cannot be restarted");

		isDispatchPaused.set(false);
	}

	public void pauseDispatch() {
		HintsService.logger.info("Paused hints dispatch");
		isDispatchPaused.set(true);
	}

	public void resumeDispatch() {
		HintsService.logger.info("Resumed hints dispatch");
		isDispatchPaused.set(false);
	}

	public synchronized void shutdownBlocking() throws InterruptedException, ExecutionException {
		if (isShutDown)
			throw new IllegalStateException("HintsService has already been shut down");

		isShutDown = true;
		if ((triggerDispatchFuture) != null)
			triggerDispatchFuture.cancel(false);

		pauseDispatch();
		triggerFlushingFuture.cancel(false);
	}

	public void deleteAllHints() {
	}

	public void deleteAllHintsForEndpoint(String address) {
		InetAddress target;
		try {
			target = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
		deleteAllHintsForEndpoint(target);
	}

	public void deleteAllHintsForEndpoint(InetAddress target) {
		UUID hostId = StorageService.instance.getHostIdForEndpoint(target);
		if (hostId == null)
			throw new IllegalArgumentException(("Can't delete hints for unknown address " + target));

	}

	public void excise(UUID hostId) {
	}

	public Future transferHints(Supplier<UUID> hostIdSupplier) {
		resumeDispatch();
		return null;
	}

	public boolean isShutDown() {
		return isShutDown;
	}
}

