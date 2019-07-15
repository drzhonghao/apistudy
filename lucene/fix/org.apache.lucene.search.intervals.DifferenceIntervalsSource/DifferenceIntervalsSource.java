

import java.io.IOException;
import java.util.Set;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.intervals.IntervalIterator;
import org.apache.lucene.search.intervals.IntervalsSource;


class DifferenceIntervalsSource extends IntervalsSource {
	final IntervalsSource minuend = null;

	final IntervalsSource subtrahend = null;

	@Override
	public IntervalIterator intervals(String field, LeafReaderContext ctx) throws IOException {
		IntervalIterator minIt = minuend.intervals(field, ctx);
		if (minIt == null)
			return null;

		IntervalIterator subIt = subtrahend.intervals(field, ctx);
		if (subIt == null)
			return minIt;

		return null;
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if ((o == null) || ((getClass()) != (o.getClass())))
			return false;

		DifferenceIntervalsSource that = ((DifferenceIntervalsSource) (o));
		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return null;
	}

	@Override
	public void extractTerms(String field, Set<Term> terms) {
		minuend.extractTerms(field, terms);
	}
}

