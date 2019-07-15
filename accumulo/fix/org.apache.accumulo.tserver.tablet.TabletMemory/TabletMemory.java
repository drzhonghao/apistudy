

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.util.LocalityGroupUtil;
import org.apache.accumulo.server.conf.TableConfiguration;
import org.apache.accumulo.tserver.InMemoryMap;
import org.apache.accumulo.tserver.tablet.CommitSession;
import org.apache.accumulo.tserver.tablet.TabletCommitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class TabletMemory implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(TabletMemory.class);

	private final TabletCommitter tablet;

	private InMemoryMap memTable;

	private InMemoryMap otherMemTable;

	private InMemoryMap deletingMemTable;

	private long nextSeq = 1L;

	private CommitSession commitSession;

	TabletMemory(TabletCommitter tablet) {
		this.tablet = tablet;
		try {
			memTable = new InMemoryMap(tablet.getTableConfiguration());
		} catch (LocalityGroupUtil.LocalityGroupConfigurationError e) {
			throw new RuntimeException(e);
		}
		nextSeq += 2;
	}

	public InMemoryMap getMemTable() {
		return memTable;
	}

	public InMemoryMap getMinCMemTable() {
		return otherMemTable;
	}

	public CommitSession prepareForMinC() {
		if ((otherMemTable) != null) {
			throw new IllegalStateException();
		}
		if ((deletingMemTable) != null) {
			throw new IllegalStateException();
		}
		if ((commitSession) == null) {
			throw new IllegalStateException();
		}
		otherMemTable = memTable;
		try {
			memTable = new InMemoryMap(tablet.getTableConfiguration());
		} catch (LocalityGroupUtil.LocalityGroupConfigurationError e) {
			throw new RuntimeException(e);
		}
		CommitSession oldCommitSession = commitSession;
		nextSeq += 2;
		tablet.updateMemoryUsageStats(memTable.estimatedSizeInBytes(), otherMemTable.estimatedSizeInBytes());
		return oldCommitSession;
	}

	public void finishedMinC() {
		if ((otherMemTable) == null) {
			throw new IllegalStateException();
		}
		if ((deletingMemTable) != null) {
			throw new IllegalStateException();
		}
		if ((commitSession) == null) {
			throw new IllegalStateException();
		}
		deletingMemTable = otherMemTable;
		otherMemTable = null;
		tablet.notifyAll();
	}

	public void finalizeMinC() {
		if ((commitSession) == null) {
			throw new IllegalStateException();
		}
		try {
			deletingMemTable.delete(15000);
		} finally {
			synchronized(tablet) {
				if ((otherMemTable) != null) {
					throw new IllegalStateException();
				}
				if ((deletingMemTable) == null) {
					throw new IllegalStateException();
				}
				deletingMemTable = null;
				tablet.updateMemoryUsageStats(memTable.estimatedSizeInBytes(), 0);
			}
		}
	}

	public boolean memoryReservedForMinC() {
		return ((otherMemTable) != null) || ((deletingMemTable) != null);
	}

	public void waitForMinC() {
		while (((otherMemTable) != null) || ((deletingMemTable) != null)) {
			try {
				tablet.wait(50);
			} catch (InterruptedException e) {
				TabletMemory.log.warn("{}", e.getMessage(), e);
			}
		} 
	}

	public void mutate(CommitSession cm, List<Mutation> mutations) {
		cm.mutate(mutations);
	}

	public void updateMemoryUsageStats() {
		long other = 0;
		if ((otherMemTable) != null)
			other = otherMemTable.estimatedSizeInBytes();
		else
			if ((deletingMemTable) != null)
				other = deletingMemTable.estimatedSizeInBytes();


		tablet.updateMemoryUsageStats(memTable.estimatedSizeInBytes(), other);
	}

	public List<InMemoryMap.MemoryIterator> getIterators(SamplerConfigurationImpl samplerConfig) {
		List<InMemoryMap.MemoryIterator> toReturn = new ArrayList<>(2);
		toReturn.add(memTable.skvIterator(samplerConfig));
		if ((otherMemTable) != null)
			toReturn.add(otherMemTable.skvIterator(samplerConfig));

		return toReturn;
	}

	public void returnIterators(List<InMemoryMap.MemoryIterator> iters) {
		for (InMemoryMap.MemoryIterator iter : iters) {
			iter.close();
		}
	}

	public long getNumEntries() {
		if ((otherMemTable) != null)
			return (memTable.getNumEntries()) + (otherMemTable.getNumEntries());

		return memTable.getNumEntries();
	}

	public CommitSession getCommitSession() {
		return commitSession;
	}

	@Override
	public void close() throws IOException {
		commitSession = null;
	}

	public boolean isClosed() {
		return (commitSession) == null;
	}
}

