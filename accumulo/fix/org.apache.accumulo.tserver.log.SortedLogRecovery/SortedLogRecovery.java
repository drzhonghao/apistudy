

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.tserver.log.MutationReceiver;
import org.apache.accumulo.tserver.logger.LogEvents;
import org.apache.accumulo.tserver.logger.LogFileKey;
import org.apache.accumulo.tserver.logger.LogFileValue;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SortedLogRecovery {
	private static final Logger log = LoggerFactory.getLogger(SortedLogRecovery.class);

	private VolumeManager fs;

	public SortedLogRecovery(VolumeManager fs) {
		this.fs = fs;
	}

	static LogFileKey maxKey(LogEvents event) {
		LogFileKey key = new LogFileKey();
		key.event = event;
		key.tabletId = Integer.MAX_VALUE;
		key.seq = Long.MAX_VALUE;
		return key;
	}

	static LogFileKey maxKey(LogEvents event, int tabletId) {
		LogFileKey key = SortedLogRecovery.maxKey(event);
		key.tabletId = tabletId;
		return key;
	}

	static LogFileKey minKey(LogEvents event) {
		LogFileKey key = new LogFileKey();
		key.event = event;
		key.tabletId = -1;
		key.seq = 0;
		return key;
	}

	static LogFileKey minKey(LogEvents event, int tabletId) {
		LogFileKey key = SortedLogRecovery.minKey(event);
		key.tabletId = tabletId;
		return key;
	}

	private int findMaxTabletId(KeyExtent extent, List<Path> recoveryLogs) throws IOException {
		int tabletId = -1;
		return tabletId;
	}

	private String getPathSuffix(String pathString) {
		Path path = new Path(pathString);
		if ((path.depth()) < 2)
			throw new IllegalArgumentException(("Bad path " + pathString));

		return ((path.getParent().getName()) + "/") + (path.getName());
	}

	static class DeduplicatingIterator implements Iterator<Map.Entry<LogFileKey, LogFileValue>> {
		private PeekingIterator<Map.Entry<LogFileKey, LogFileValue>> source;

		public DeduplicatingIterator(Iterator<Map.Entry<LogFileKey, LogFileValue>> source) {
			this.source = Iterators.peekingIterator(source);
		}

		@Override
		public boolean hasNext() {
			return source.hasNext();
		}

		@Override
		public Map.Entry<LogFileKey, LogFileValue> next() {
			Map.Entry<LogFileKey, LogFileValue> next = source.next();
			while ((source.hasNext()) && ((next.getKey().compareTo(source.peek().getKey())) == 0)) {
				source.next();
			} 
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}

	private long findRecoverySeq(List<Path> recoveryLogs, Set<String> tabletFiles, int tabletId) throws IOException {
		HashSet<String> suffixes = new HashSet<>();
		for (String path : tabletFiles)
			suffixes.add(getPathSuffix(path));

		long lastStart = 0;
		long lastFinish = 0;
		long recoverySeq = 0;
		return recoverySeq;
	}

	private void playbackMutations(List<Path> recoveryLogs, MutationReceiver mr, int tabletId, long recoverySeq) throws IOException {
		LogFileKey start = SortedLogRecovery.minKey(LogEvents.MUTATION, tabletId);
		start.seq = recoverySeq;
		LogFileKey end = SortedLogRecovery.maxKey(LogEvents.MUTATION, tabletId);
	}

	Collection<String> asNames(List<Path> recoveryLogs) {
		return Collections2.transform(recoveryLogs, new Function<Path, String>() {
			@Override
			public String apply(Path input) {
				return input.getName();
			}
		});
	}

	public void recover(KeyExtent extent, List<Path> recoveryLogs, Set<String> tabletFiles, MutationReceiver mr) throws IOException {
		int tabletId = findMaxTabletId(extent, recoveryLogs);
		if (tabletId == (-1)) {
			SortedLogRecovery.log.info("Tablet {} is not defined in recovery logs {} ", extent, asNames(recoveryLogs));
			return;
		}
		long recoverySeq = findRecoverySeq(recoveryLogs, tabletFiles, tabletId);
		SortedLogRecovery.log.info("Recovering mutations, tablet:{} tabletId:{} seq:{} logs:{}");
		playbackMutations(recoveryLogs, mr, tabletId, recoverySeq);
	}
}

