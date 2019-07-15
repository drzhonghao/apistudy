

import java.util.concurrent.atomic.AtomicReference;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.TKeyExtent;
import org.apache.accumulo.core.iterators.IterationInterruptedException;
import org.apache.accumulo.core.tabletserver.thrift.NotServingTabletException;
import org.apache.accumulo.core.util.Stat;
import org.apache.accumulo.server.util.Halt;
import org.apache.accumulo.tserver.TabletServer;
import org.apache.accumulo.tserver.TooManyFilesException;
import org.apache.accumulo.tserver.scan.ScanRunState;
import org.apache.accumulo.tserver.scan.ScanTask;
import org.apache.accumulo.tserver.session.ScanSession;
import org.apache.accumulo.tserver.session.Session;
import org.apache.accumulo.tserver.tablet.ScanBatch;
import org.apache.accumulo.tserver.tablet.Scanner;
import org.apache.accumulo.tserver.tablet.Tablet;
import org.apache.accumulo.tserver.tablet.TabletClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NextBatchTask extends ScanTask<ScanBatch> {
	private static final Logger log = LoggerFactory.getLogger(NextBatchTask.class);

	private final long scanID;

	@Override
	public void run() {
		final ScanSession scanSession = ((ScanSession) (server.getSession(scanID)));
		String oldThreadName = Thread.currentThread().getName();
		try {
			if ((isCancelled()) || (scanSession == null))
				return;

			runState.set(ScanRunState.RUNNING);
			Thread.currentThread().setName(((((((("User: " + (scanSession.getUser())) + " Start: ") + (scanSession.startTime)) + " Client: ") + (scanSession.client)) + " Tablet: ") + (scanSession.extent)));
			Tablet tablet = server.getOnlineTablet(scanSession.extent);
			if (tablet == null) {
				addResult(new NotServingTabletException(scanSession.extent.toThrift()));
				return;
			}
			long t1 = System.currentTimeMillis();
			ScanBatch batch = scanSession.scanner.read();
			long t2 = System.currentTimeMillis();
			scanSession.nbTimes.addStat((t2 - t1));
			addResult(batch);
		} catch (TabletClosedException e) {
			addResult(new NotServingTabletException(scanSession.extent.toThrift()));
		} catch (IterationInterruptedException iie) {
			if (!(isCancelled())) {
				NextBatchTask.log.warn("Iteration interrupted, when scan not cancelled", iie);
				addResult(iie);
			}
		} catch (TooManyFilesException | SampleNotPresentException e) {
			addResult(e);
		} catch (OutOfMemoryError ome) {
			Halt.halt(((("Ran out of memory scanning " + (scanSession.extent)) + " for ") + (scanSession.client)), 1);
			addResult(ome);
		} catch (Throwable e) {
			NextBatchTask.log.warn(("exception while scanning tablet " + (scanSession == null ? "(unknown)" : scanSession.extent)), e);
			addResult(e);
		} finally {
			runState.set(ScanRunState.FINISHED);
			Thread.currentThread().setName(oldThreadName);
		}
	}
}

