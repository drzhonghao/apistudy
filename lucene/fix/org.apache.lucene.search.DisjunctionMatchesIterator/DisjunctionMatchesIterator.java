

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.MatchesIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.PriorityQueue;


final class DisjunctionMatchesIterator implements MatchesIterator {
	static MatchesIterator fromTerms(LeafReaderContext context, int doc, String field, List<Term> terms) throws IOException {
		Objects.requireNonNull(field);
		for (Term term : terms) {
			if ((Objects.equals(field, term.field())) == false) {
				throw new IllegalArgumentException((((("Tried to generate iterator from terms in multiple fields: expected [" + field) + "] but got [") + (term.field())) + "]"));
			}
		}
		return DisjunctionMatchesIterator.fromTermsEnum(context, doc, field, DisjunctionMatchesIterator.asBytesRefIterator(terms));
	}

	private static BytesRefIterator asBytesRefIterator(List<Term> terms) {
		return new BytesRefIterator() {
			int i = 0;

			@Override
			public BytesRef next() {
				if ((i) >= (terms.size()))
					return null;

				return terms.get(((i)++)).bytes();
			}
		};
	}

	static MatchesIterator fromTermsEnum(LeafReaderContext context, int doc, String field, BytesRefIterator terms) throws IOException {
		Objects.requireNonNull(field);
		List<MatchesIterator> mis = new ArrayList<>();
		Terms t = context.reader().terms(field);
		if (t == null)
			return null;

		TermsEnum te = t.iterator();
		PostingsEnum reuse = null;
		for (BytesRef term = terms.next(); term != null; term = terms.next()) {
			if (te.seekExact(term)) {
				PostingsEnum pe = te.postings(reuse, PostingsEnum.OFFSETS);
				if ((pe.advance(doc)) == doc) {
					reuse = null;
				}else {
					reuse = pe;
				}
			}
		}
		return DisjunctionMatchesIterator.fromSubIterators(mis);
	}

	static MatchesIterator fromSubIterators(List<MatchesIterator> mis) throws IOException {
		if ((mis.size()) == 0)
			return null;

		if ((mis.size()) == 1)
			return mis.get(0);

		return new DisjunctionMatchesIterator(mis);
	}

	private final PriorityQueue<MatchesIterator> queue;

	private boolean started = false;

	private DisjunctionMatchesIterator(List<MatchesIterator> matches) throws IOException {
		queue = new PriorityQueue<MatchesIterator>(matches.size()) {
			@Override
			protected boolean lessThan(MatchesIterator a, MatchesIterator b) {
				return (((a.startPosition()) < (b.startPosition())) || (((a.startPosition()) == (b.startPosition())) && ((a.endPosition()) < (b.endPosition())))) || (((a.startPosition()) == (b.startPosition())) && ((a.endPosition()) == (b.endPosition())));
			}
		};
		for (MatchesIterator mi : matches) {
			if (mi.next()) {
				queue.add(mi);
			}
		}
	}

	@Override
	public boolean next() throws IOException {
		if ((started) == false) {
			return started = true;
		}
		if ((queue.top().next()) == false) {
			queue.pop();
		}
		if ((queue.size()) > 0) {
			queue.updateTop();
			return true;
		}
		return false;
	}

	@Override
	public int startPosition() {
		return queue.top().startPosition();
	}

	@Override
	public int endPosition() {
		return queue.top().endPosition();
	}

	@Override
	public int startOffset() throws IOException {
		return queue.top().startOffset();
	}

	@Override
	public int endOffset() throws IOException {
		return queue.top().endOffset();
	}
}

