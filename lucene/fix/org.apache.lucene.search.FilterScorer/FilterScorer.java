

import java.io.IOException;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;


public abstract class FilterScorer extends Scorer {
	protected final Scorer in;

	public FilterScorer(Scorer in, Weight weight) {
		super(weight);
		if (in == null) {
			throw new NullPointerException("wrapped Scorer must not be null");
		}
		this.in = in;
	}

	@Override
	public float score() throws IOException {
		return in.score();
	}

	@Override
	public final int docID() {
		return in.docID();
	}

	@Override
	public final DocIdSetIterator iterator() {
		return in.iterator();
	}

	@Override
	public final TwoPhaseIterator twoPhaseIterator() {
		return in.twoPhaseIterator();
	}
}

