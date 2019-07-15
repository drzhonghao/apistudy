

import com.google.common.base.Function;
import java.util.Collection;
import java.util.UUID;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterators;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.io.sstable.ReducingKeyIterator;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.repair.SystemDistributedKeyspace;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.cassandra.utils.concurrent.Refs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ViewBuilder extends CompactionInfo.Holder {
	private final ColumnFamilyStore baseCfs;

	private final View view;

	private final UUID compactionId;

	private volatile Token prevToken = null;

	private static final Logger logger = LoggerFactory.getLogger(ViewBuilder.class);

	private volatile boolean isStopped = false;

	public ViewBuilder(ColumnFamilyStore baseCfs, View view) {
		this.baseCfs = baseCfs;
		this.view = view;
		compactionId = UUIDGen.getTimeUUID();
	}

	private void buildKey(DecoratedKey key) {
		int nowInSec = FBUtilities.nowInSeconds();
		UnfilteredRowIterator empty = UnfilteredRowIterators.noRowsIterator(baseCfs.metadata, key, Rows.EMPTY_STATIC_ROW, DeletionTime.LIVE, false);
	}

	public void run() {
		UUID localHostId = SystemKeyspace.getLocalHostId();
		String ksname = baseCfs.metadata.ksName;
		Iterable<Range<Token>> ranges = StorageService.instance.getLocalRanges(baseCfs.metadata.ksName);
		Token lastToken;
		Function<View, Iterable<SSTableReader>> function;
		baseCfs.forceBlockingFlush();
		function = View.selectFunction(SSTableSet.CANONICAL);
		lastToken = null;
		prevToken = lastToken;
		long keysBuilt = 0;
		try (final Refs<SSTableReader> sstables = baseCfs.selectAndReference(function).refs;final ReducingKeyIterator iter = new ReducingKeyIterator(sstables)) {
			while ((!(isStopped)) && (iter.hasNext())) {
				DecoratedKey key = iter.next();
				Token token = key.getToken();
				if ((lastToken == null) || ((lastToken.compareTo(token)) < 0)) {
					for (Range<Token> range : ranges) {
						if (range.contains(token)) {
							buildKey(key);
							++keysBuilt;
							if (((prevToken) == null) || ((prevToken.compareTo(token)) != 0)) {
								prevToken = token;
							}
						}
					}
					lastToken = null;
				}
			} 
			if (!(isStopped)) {
			}else {
			}
		} catch (Exception e) {
			ViewBuilder.logger.warn("Materialized View failed to complete, sleeping 5 minutes before restarting", e);
		}
	}

	private void updateDistributed(String ksname, String viewName, UUID localHostId) {
		try {
			SystemDistributedKeyspace.successfulViewBuild(ksname, viewName, localHostId);
			SystemKeyspace.setViewBuiltReplicated(ksname, viewName);
		} catch (Exception e) {
			ViewBuilder.logger.warn("Failed to updated the distributed status of view, sleeping 5 minutes before retrying", e);
		}
	}

	public CompactionInfo getCompactionInfo() {
		long rangesLeft = 0;
		long rangesTotal = 0;
		Token lastToken = prevToken;
		for (Range<Token> range : StorageService.instance.getLocalRanges(baseCfs.keyspace.getName())) {
			rangesLeft++;
			rangesTotal++;
			if ((lastToken == null) || (range.contains(lastToken)))
				rangesLeft = 0;

		}
		return new CompactionInfo(baseCfs.metadata, OperationType.VIEW_BUILD, rangesLeft, rangesTotal, "ranges", compactionId);
	}

	public void stop() {
		isStopped = true;
	}
}

