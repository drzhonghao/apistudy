

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.util.ratelimit.RateLimiter;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.server.conf.TableConfiguration;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.tserver.InMemoryMap;
import org.apache.accumulo.tserver.MinorCompactionReason;
import org.apache.accumulo.tserver.TabletServer;
import org.apache.accumulo.tserver.tablet.CompactionStats;
import org.apache.accumulo.tserver.tablet.Compactor;
import org.apache.accumulo.tserver.tablet.Tablet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.minc;


public class MinorCompactor extends Compactor {
	private static final Logger log = LoggerFactory.getLogger(MinorCompactor.class);

	private static final Map<FileRef, DataFileValue> EMPTY_MAP = Collections.emptyMap();

	private static Map<FileRef, DataFileValue> toFileMap(FileRef mergeFile, DataFileValue dfv) {
		if (mergeFile == null)
			return MinorCompactor.EMPTY_MAP;

		return Collections.singletonMap(mergeFile, dfv);
	}

	private final TabletServer tabletServer;

	public MinorCompactor(TabletServer tabletServer, Tablet tablet, InMemoryMap imm, FileRef mergeFile, DataFileValue dfv, FileRef outputFile, MinorCompactionReason mincReason, TableConfiguration tableConfig) {
		super(tabletServer, tablet, MinorCompactor.toFileMap(mergeFile, dfv), imm, outputFile, true, new Compactor.CompactionEnv() {
			@Override
			public boolean isCompactionEnabled() {
				return true;
			}

			@Override
			public IteratorUtil.IteratorScope getIteratorScope() {
				return minc;
			}

			@Override
			public RateLimiter getReadLimiter() {
				return null;
			}

			@Override
			public RateLimiter getWriteLimiter() {
				return null;
			}
		}, Collections.<IteratorSetting>emptyList(), mincReason.ordinal(), tableConfig);
		this.tabletServer = tabletServer;
	}

	private boolean isTableDeleting() {
		try {
			return (Tables.getTableState(tabletServer.getInstance(), extent.getTableId())) == (TableState.DELETING);
		} catch (Exception e) {
			MinorCompactor.log.warn((("Failed to determine if table " + (extent.getTableId())) + " was deleting "), e);
			return false;
		}
	}

	@Override
	public CompactionStats call() {
		int sleepTime = 100;
		double growthFactor = 4;
		int maxSleepTime = (1000 * 60) * 3;
		boolean reportedProblem = false;
		Compactor.runningCompactions.add(this);
		try {
			do {
				try {
					CompactionStats ret = super.call();
					if (reportedProblem) {
					}
					return ret;
				} catch (IOException e) {
					reportedProblem = true;
				} catch (RuntimeException e) {
					reportedProblem = true;
				} catch (Compactor.CompactionCanceledException e) {
					throw new IllegalStateException(e);
				}
				Random random = new Random();
				int sleep = sleepTime + (random.nextInt(sleepTime));
				MinorCompactor.log.debug((("MinC failed sleeping " + sleep) + " ms before retrying"));
				UtilWaitThread.sleepUninterruptibly(sleep, TimeUnit.MILLISECONDS);
				sleepTime = ((int) (Math.round(Math.min(maxSleepTime, (sleepTime * growthFactor)))));
				if (isTableDeleting()) {
				}
			} while (true );
		} finally {
			thread = null;
			Compactor.runningCompactions.remove(this);
		}
	}
}

