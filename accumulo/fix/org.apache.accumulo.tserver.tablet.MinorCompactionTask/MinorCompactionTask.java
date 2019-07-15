

import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.trace.ProbabilitySampler;
import org.apache.accumulo.core.trace.Span;
import org.apache.accumulo.core.trace.Trace;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.tserver.MinorCompactionReason;
import org.apache.accumulo.tserver.compaction.MajorCompactionReason;
import org.apache.accumulo.tserver.tablet.CommitSession;
import org.apache.accumulo.tserver.tablet.Tablet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class MinorCompactionTask implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MinorCompactionTask.class);

	private final Tablet tablet;

	private long queued;

	private CommitSession commitSession;

	private DataFileValue stats;

	private FileRef mergeFile;

	private long flushId;

	private MinorCompactionReason mincReason;

	private double tracePercent;

	MinorCompactionTask(Tablet tablet, FileRef mergeFile, CommitSession commitSession, long flushId, MinorCompactionReason mincReason, double tracePercent) {
		this.tablet = tablet;
		queued = System.currentTimeMillis();
		tablet.minorCompactionWaitingToStart();
		this.commitSession = commitSession;
		this.mergeFile = mergeFile;
		this.flushId = flushId;
		this.mincReason = mincReason;
		this.tracePercent = tracePercent;
	}

	@Override
	public void run() {
		tablet.minorCompactionStarted();
		ProbabilitySampler sampler = new ProbabilitySampler(tracePercent);
		Span minorCompaction = Trace.on("minorCompaction", sampler);
		try {
			Span span = Trace.start("waitForCommits");
			synchronized(tablet) {
				commitSession.waitForCommitsToFinish();
			}
			span.stop();
			span = Trace.start("start");
			while (true) {
				break;
			} 
			span.stop();
			span = Trace.start("compact");
			span.stop();
			minorCompaction.data("extent", tablet.getExtent().toString());
			minorCompaction.data("numEntries", Long.toString(this.stats.getNumEntries()));
			minorCompaction.data("size", Long.toString(this.stats.getSize()));
			minorCompaction.stop();
			if (tablet.needsSplit()) {
			}else {
				tablet.initiateMajorCompaction(MajorCompactionReason.NORMAL);
			}
		} catch (Throwable t) {
			MinorCompactionTask.log.error(("Unknown error during minor compaction for extent: " + (tablet.getExtent())), t);
			throw new RuntimeException(t);
		} finally {
			tablet.minorCompactionComplete();
			minorCompaction.stop();
		}
	}
}

