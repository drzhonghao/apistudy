

import java.util.Objects;
import org.apache.accumulo.tserver.compaction.MajorCompactionReason;
import org.apache.accumulo.tserver.tablet.Tablet;


final class CompactionRunner implements Comparable<CompactionRunner> , Runnable {
	private final Tablet tablet;

	private final MajorCompactionReason reason;

	private final long queued;

	public CompactionRunner(Tablet tablet, MajorCompactionReason reason) {
		this.tablet = tablet;
		queued = System.currentTimeMillis();
		this.reason = reason;
	}

	@Override
	public void run() {
		synchronized(tablet) {
			if (((reason) == (MajorCompactionReason.NORMAL)) && (tablet.needsMajorCompaction(reason)))
				tablet.initiateMajorCompaction(reason);

		}
	}

	private int getNumFiles() {
		return 0;
	}

	@Override
	public int hashCode() {
		return ((Objects.hashCode(reason)) + (Objects.hashCode(queued))) + (getNumFiles());
	}

	@Override
	public boolean equals(Object obj) {
		return ((this) == obj) || (((obj != null) && (obj instanceof CompactionRunner)) && (0 == (compareTo(((CompactionRunner) (obj))))));
	}

	@Override
	public int compareTo(CompactionRunner o) {
		int cmp = reason.compareTo(o.reason);
		if (cmp != 0)
			return cmp;

		if (((reason) == (MajorCompactionReason.USER)) || ((reason) == (MajorCompactionReason.CHOP))) {
			cmp = ((int) ((queued) - (o.queued)));
			if (cmp != 0)
				return cmp;

		}
		return (o.getNumFiles()) - (this.getNumFiles());
	}
}

