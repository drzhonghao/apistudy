

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;


public final class TermContext {
	private final Object topReaderContextIdentity;

	private final TermState[] states;

	private int docFreq;

	private long totalTermFreq;

	public TermContext(IndexReaderContext context) {
		assert (context != null) && (context.isTopLevel);
		docFreq = 0;
		totalTermFreq = 0;
		final int len;
		if ((context.leaves()) == null) {
			len = 1;
		}else {
			len = context.leaves().size();
		}
		states = new TermState[len];
		topReaderContextIdentity = null;
	}

	public boolean wasBuiltFor(IndexReaderContext context) {
		return false;
	}

	public TermContext(IndexReaderContext context, TermState state, int ord, int docFreq, long totalTermFreq) {
		this(context);
		register(state, ord, docFreq, totalTermFreq);
	}

	public static TermContext build(IndexReaderContext context, Term term) throws IOException {
		assert (context != null) && (context.isTopLevel);
		final String field = term.field();
		final BytesRef bytes = term.bytes();
		final TermContext perReaderTermState = new TermContext(context);
		for (final LeafReaderContext ctx : context.leaves()) {
			final Terms terms = ctx.reader().terms(field);
			if (terms != null) {
				final TermsEnum termsEnum = terms.iterator();
				if (termsEnum.seekExact(bytes)) {
					final TermState termState = termsEnum.termState();
					perReaderTermState.register(termState, ctx.ord, termsEnum.docFreq(), termsEnum.totalTermFreq());
				}
			}
		}
		return perReaderTermState;
	}

	public void clear() {
		docFreq = 0;
		totalTermFreq = 0;
		Arrays.fill(states, null);
	}

	public void register(TermState state, final int ord, final int docFreq, final long totalTermFreq) {
		register(state, ord);
		accumulateStatistics(docFreq, totalTermFreq);
	}

	public void register(TermState state, final int ord) {
		assert state != null : "state must not be null";
		assert (ord >= 0) && (ord < (states.length));
		assert (states[ord]) == null : ("state for ord: " + ord) + " already registered";
		states[ord] = state;
	}

	public void accumulateStatistics(final int docFreq, final long totalTermFreq) {
		this.docFreq += docFreq;
		if (((this.totalTermFreq) >= 0) && (totalTermFreq >= 0))
			this.totalTermFreq += totalTermFreq;
		else
			this.totalTermFreq = -1;

	}

	public TermState get(int ord) {
		assert (ord >= 0) && (ord < (states.length));
		return states[ord];
	}

	public int docFreq() {
		return docFreq;
	}

	public long totalTermFreq() {
		return totalTermFreq;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TermContext\n");
		for (TermState termState : states) {
			sb.append("  state=");
			sb.append(termState);
			sb.append('\n');
		}
		return sb.toString();
	}
}

