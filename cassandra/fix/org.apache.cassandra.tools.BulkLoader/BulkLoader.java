

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.SSLOptions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractFuture;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Set;
import javax.net.ssl.SSLContext;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.io.sstable.SSTableLoader;
import org.apache.cassandra.security.SSLFactory;
import org.apache.cassandra.streaming.ProgressInfo;
import org.apache.cassandra.streaming.SessionInfo;
import org.apache.cassandra.streaming.StreamConnectionFactory;
import org.apache.cassandra.streaming.StreamEvent;
import org.apache.cassandra.streaming.StreamEventHandler;
import org.apache.cassandra.streaming.StreamResultFuture;
import org.apache.cassandra.streaming.StreamState;
import org.apache.cassandra.tools.BulkLoadConnectionFactory;
import org.apache.cassandra.tools.BulkLoadException;
import org.apache.cassandra.tools.LoaderOptions;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.NativeSSTableLoaderClient;
import org.apache.cassandra.utils.OutputHandler;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import static org.apache.cassandra.streaming.StreamEvent.Type.FILE_PROGRESS;
import static org.apache.cassandra.streaming.StreamEvent.Type.STREAM_COMPLETE;
import static org.apache.cassandra.streaming.StreamEvent.Type.STREAM_PREPARED;


public class BulkLoader {
	public static void main(String[] args) throws BulkLoadException {
	}

	public static void load(LoaderOptions options) throws BulkLoadException {
		DatabaseDescriptor.toolInitialization();
		OutputHandler handler = new OutputHandler.SystemOutput(options.verbose, options.debug);
		SSTableLoader loader = new SSTableLoader(options.directory.getAbsoluteFile(), new BulkLoader.ExternalClient(options.hosts, options.nativePort, options.authProvider, options.storagePort, options.sslStoragePort, options.serverEncOptions, BulkLoader.buildSSLOptions(options.clientEncOptions)), handler, options.connectionsPerHost);
		DatabaseDescriptor.setStreamThroughputOutboundMegabitsPerSec(options.throttle);
		DatabaseDescriptor.setInterDCStreamThroughputOutboundMegabitsPerSec(options.interDcThrottle);
		StreamResultFuture future = null;
		BulkLoader.ProgressIndicator indicator = new BulkLoader.ProgressIndicator();
		try {
			if (options.noProgress) {
				future = loader.stream(options.ignores);
			}else {
				future = loader.stream(options.ignores, indicator);
			}
		} catch (Exception e) {
			JVMStabilityInspector.inspectThrowable(e);
			System.err.println(e.getMessage());
			if ((e.getCause()) != null) {
				System.err.println(e.getCause());
			}
			e.printStackTrace(System.err);
			throw new BulkLoadException(e);
		}
		try {
			future.get();
			if (!(options.noProgress)) {
				indicator.printSummary(options.connectionsPerHost);
			}
			Thread.sleep(1000);
		} catch (Exception e) {
			System.err.println("Streaming to the following hosts failed:");
			System.err.println(loader.getFailedHosts());
			e.printStackTrace(System.err);
			throw new BulkLoadException(e);
		}
	}

	static class ProgressIndicator implements StreamEventHandler {
		private long start;

		private long lastProgress;

		private long lastTime;

		private long peak = 0;

		private int totalFiles = 0;

		private final Multimap<InetAddress, SessionInfo> sessionsByHost = HashMultimap.create();

		public ProgressIndicator() {
			start = lastTime = System.nanoTime();
		}

		public void onSuccess(StreamState finalState) {
		}

		public void onFailure(Throwable t) {
		}

		public synchronized void handleStreamEvent(StreamEvent event) {
			if ((event.eventType) == (STREAM_PREPARED)) {
				SessionInfo session = ((StreamEvent.SessionPreparedEvent) (event)).session;
				sessionsByHost.put(session.peer, session);
			}else
				if (((event.eventType) == (FILE_PROGRESS)) || ((event.eventType) == (STREAM_COMPLETE))) {
					ProgressInfo progressInfo = null;
					if ((event.eventType) == (FILE_PROGRESS)) {
						progressInfo = ((StreamEvent.ProgressEvent) (event)).progress;
					}
					long time = System.nanoTime();
					long deltaTime = time - (lastTime);
					StringBuilder sb = new StringBuilder();
					sb.append("\rprogress: ");
					long totalProgress = 0;
					long totalSize = 0;
					boolean updateTotalFiles = (totalFiles) == 0;
					for (InetAddress peer : sessionsByHost.keySet()) {
						sb.append("[").append(peer).append("]");
						for (SessionInfo session : sessionsByHost.get(peer)) {
							long size = session.getTotalSizeToSend();
							long current = 0;
							int completed = 0;
							if (((progressInfo != null) && (session.peer.equals(progressInfo.peer))) && ((session.sessionIndex) == (progressInfo.sessionIndex))) {
								session.updateProgress(progressInfo);
							}
							for (ProgressInfo progress : session.getSendingFiles()) {
								if (progress.isCompleted()) {
									completed++;
								}
								current += progress.currentBytes;
							}
							totalProgress += current;
							totalSize += size;
							sb.append(session.sessionIndex).append(":");
							sb.append(completed).append("/").append(session.getTotalFilesToSend());
							sb.append(" ").append(String.format("%-3d", (size == 0 ? 100L : (current * 100L) / size))).append("% ");
							if (updateTotalFiles) {
								totalFiles += session.getTotalFilesToSend();
							}
						}
					}
					lastTime = time;
					long deltaProgress = totalProgress - (lastProgress);
					lastProgress = totalProgress;
					sb.append("total: ").append((totalSize == 0 ? 100L : (totalProgress * 100L) / totalSize)).append("% ");
					sb.append(FBUtilities.prettyPrintMemoryPerSecond(deltaProgress, deltaTime));
					long average = bytesPerSecond(totalProgress, (time - (start)));
					if (average > (peak)) {
						peak = average;
					}
					sb.append(" (avg: ").append(FBUtilities.prettyPrintMemoryPerSecond(totalProgress, (time - (start)))).append(")");
					System.out.println(sb.toString());
				}

		}

		private long bytesPerSecond(long bytes, long timeInNano) {
			return timeInNano != 0 ? ((long) ((((((double) (bytes)) / timeInNano) * 1000) * 1000) * 1000)) : 0;
		}

		private void printSummary(int connectionsPerHost) {
			long end = System.nanoTime();
			long durationMS = (end - (start)) / 1000000;
			StringBuilder sb = new StringBuilder();
			sb.append("\nSummary statistics: \n");
			sb.append(String.format("   %-24s: %-10d%n", "Connections per host ", connectionsPerHost));
			sb.append(String.format("   %-24s: %-10d%n", "Total files transferred ", totalFiles));
			sb.append(String.format("   %-24s: %-10s%n", "Total bytes transferred ", FBUtilities.prettyPrintMemory(lastProgress)));
			sb.append(String.format("   %-24s: %-10s%n", "Total duration ", (durationMS + " ms")));
			sb.append(String.format("   %-24s: %-10s%n", "Average transfer rate ", FBUtilities.prettyPrintMemoryPerSecond(lastProgress, (end - (start)))));
			sb.append(String.format("   %-24s: %-10s%n", "Peak transfer rate ", FBUtilities.prettyPrintMemoryPerSecond(peak)));
			System.out.println(sb.toString());
		}
	}

	private static SSLOptions buildSSLOptions(EncryptionOptions.ClientEncryptionOptions clientEncryptionOptions) {
		if (!(clientEncryptionOptions.enabled)) {
			return null;
		}
		SSLContext sslContext;
		try {
			sslContext = SSLFactory.createSSLContext(clientEncryptionOptions, true);
		} catch (IOException e) {
			throw new RuntimeException("Could not create SSL Context.", e);
		}
		return JdkSSLOptions.builder().withSSLContext(sslContext).withCipherSuites(clientEncryptionOptions.cipher_suites).build();
	}

	static class ExternalClient extends NativeSSTableLoaderClient {
		private final int storagePort;

		private final int sslStoragePort;

		private final EncryptionOptions.ServerEncryptionOptions serverEncOptions;

		public ExternalClient(Set<InetAddress> hosts, int port, AuthProvider authProvider, int storagePort, int sslStoragePort, EncryptionOptions.ServerEncryptionOptions serverEncryptionOptions, SSLOptions sslOptions) {
			super(hosts, port, authProvider, sslOptions);
			this.storagePort = storagePort;
			this.sslStoragePort = sslStoragePort;
			serverEncOptions = serverEncryptionOptions;
		}

		@Override
		public StreamConnectionFactory getConnectionFactory() {
			return new BulkLoadConnectionFactory(storagePort, sslStoragePort, serverEncOptions, false);
		}
	}

	public static class CmdLineOptions extends Options {
		public Options addOption(String opt, String longOpt, String argName, String description) {
			Option option = new Option(opt, longOpt, true, description);
			option.setArgName(argName);
			return addOption(option);
		}

		public Options addOption(String opt, String longOpt, String description) {
			return addOption(new Option(opt, longOpt, false, description));
		}
	}
}

