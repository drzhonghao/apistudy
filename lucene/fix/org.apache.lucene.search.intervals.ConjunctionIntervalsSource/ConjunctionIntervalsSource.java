

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.intervals.IntervalIterator;
import org.apache.lucene.search.intervals.IntervalsSource;


class ConjunctionIntervalsSource extends IntervalsSource {
	final List<IntervalsSource> subSources = null;

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if ((o == null) || ((getClass()) != (o.getClass())))
			return false;

		ConjunctionIntervalsSource that = ((ConjunctionIntervalsSource) (o));
		return false;
	}

	@Override
	public String toString() {
		return null;
	}

	@Override
	public void extractTerms(String field, Set<Term> terms) {
		for (IntervalsSource source : subSources) {
			source.extractTerms(field, terms);
		}
	}

	@Override
	public IntervalIterator intervals(String field, LeafReaderContext ctx) throws IOException {
		List<IntervalIterator> subIntervals = new ArrayList<>();
		for (IntervalsSource source : subSources) {
			IntervalIterator it = source.intervals(field, ctx);
			if (it == null)
				return null;

			subIntervals.add(it);
		}
		return null;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}

