

import java.io.IOException;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.suggest.document.CompletionWeight;
import org.apache.lucene.search.suggest.document.NRTSuggester;
import org.apache.lucene.search.suggest.document.TopSuggestDocsCollector;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.automaton.Automaton;


public class CompletionScorer extends BulkScorer {
	private final NRTSuggester suggester;

	private final Bits filterDocs;

	protected final CompletionWeight weight;

	final LeafReader reader;

	final boolean filtered;

	final Automaton automaton;

	protected CompletionScorer(final CompletionWeight weight, final NRTSuggester suggester, final LeafReader reader, final Bits filterDocs, final boolean filtered, final Automaton automaton) throws IOException {
		this.weight = weight;
		this.suggester = suggester;
		this.reader = reader;
		this.automaton = automaton;
		this.filtered = filtered;
		this.filterDocs = filterDocs;
	}

	@Override
	public int score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
		if (!(collector instanceof TopSuggestDocsCollector)) {
			throw new IllegalArgumentException("collector is not of type TopSuggestDocsCollector");
		}
		return max;
	}

	@Override
	public long cost() {
		return 0;
	}

	public final boolean accept(int docID, Bits liveDocs) {
		return (((filterDocs) == null) || (filterDocs.get(docID))) && ((liveDocs == null) || (liveDocs.get(docID)));
	}

	public float score(float weight, float boost) {
		if (boost == 0.0F) {
			return weight;
		}
		if (weight == 0.0F) {
			return boost;
		}
		return weight * boost;
	}
}

