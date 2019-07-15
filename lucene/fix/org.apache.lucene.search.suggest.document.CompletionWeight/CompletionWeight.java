

import java.io.IOException;
import java.util.Set;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.suggest.BitsProducer;
import org.apache.lucene.search.suggest.document.CompletionQuery;
import org.apache.lucene.search.suggest.document.CompletionTerms;
import org.apache.lucene.search.suggest.document.NRTSuggester;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.automaton.Automaton;


public class CompletionWeight extends Weight {
	private final CompletionQuery completionQuery;

	private final Automaton automaton;

	public CompletionWeight(final CompletionQuery query, final Automaton automaton) throws IOException {
		super(query);
		this.completionQuery = query;
		this.automaton = automaton;
	}

	public Automaton getAutomaton() {
		return automaton;
	}

	@Override
	public BulkScorer bulkScorer(final LeafReaderContext context) throws IOException {
		final LeafReader reader = context.reader();
		final Terms terms;
		final NRTSuggester suggester;
		if ((terms = reader.terms(completionQuery.getField())) == null) {
			return null;
		}
		if (terms instanceof CompletionTerms) {
			CompletionTerms completionTerms = ((CompletionTerms) (terms));
			if ((suggester = completionTerms.suggester()) == null) {
				return null;
			}
		}else {
			throw new IllegalArgumentException(((completionQuery.getField()) + " is not a SuggestField"));
		}
		BitsProducer filter = completionQuery.getFilter();
		Bits filteredDocs = null;
		if (filter != null) {
			filteredDocs = filter.getBits(context);
			if ((filteredDocs.getClass()) == (Bits.MatchNoBits.class)) {
				return null;
			}
		}
		return null;
	}

	protected void setNextMatch(IntsRef pathPrefix) {
	}

	protected float boost() {
		return 0;
	}

	protected CharSequence context() {
		return null;
	}

	@Override
	public Scorer scorer(LeafReaderContext context) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx) {
		return true;
	}

	@Override
	public void extractTerms(Set<Term> terms) {
	}

	@Override
	public Explanation explain(LeafReaderContext context, int doc) throws IOException {
		return null;
	}
}

