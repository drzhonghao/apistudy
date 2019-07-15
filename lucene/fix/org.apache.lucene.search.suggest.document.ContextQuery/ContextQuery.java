

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.lucene.analysis.miscellaneous.ConcatenateGraphFilter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.suggest.BitsProducer;
import org.apache.lucene.search.suggest.document.CompletionQuery;
import org.apache.lucene.search.suggest.document.CompletionWeight;
import org.apache.lucene.search.suggest.document.ContextSuggestField;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.fst.Util;


public class ContextQuery extends CompletionQuery {
	private IntsRefBuilder scratch = new IntsRefBuilder();

	private Map<IntsRef, ContextQuery.ContextMetaData> contexts;

	private boolean matchAllContexts = false;

	protected CompletionQuery innerQuery;

	public ContextQuery(CompletionQuery query) {
		super(query.getTerm(), query.getFilter());
		if (query instanceof ContextQuery) {
			throw new IllegalArgumentException(("'query' parameter must not be of type " + (this.getClass().getSimpleName())));
		}
		this.innerQuery = query;
		contexts = new HashMap<>();
	}

	public void addContext(CharSequence context) {
		addContext(context, 1.0F, true);
	}

	public void addContext(CharSequence context, float boost) {
		addContext(context, boost, true);
	}

	public void addContext(CharSequence context, float boost, boolean exact) {
		if (boost < 0.0F) {
			throw new IllegalArgumentException("'boost' must be >= 0");
		}
		for (int i = 0; i < (context.length()); i++) {
			if ((ContextSuggestField.CONTEXT_SEPARATOR) == (context.charAt(i))) {
				throw new IllegalArgumentException((((((("Illegal value [" + context) + "] UTF-16 codepoint [0x") + (Integer.toHexString(((int) (context.charAt(i)))))) + "] at position ") + i) + " is a reserved character"));
			}
		}
		contexts.put(IntsRef.deepCopyOf(Util.toIntsRef(new BytesRef(context), scratch)), new ContextQuery.ContextMetaData(boost, exact));
	}

	public void addAllContexts() {
		matchAllContexts = true;
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		BytesRefBuilder scratch = new BytesRefBuilder();
		for (IntsRef context : contexts.keySet()) {
			if ((buffer.length()) != 0) {
				buffer.append(",");
			}else {
				buffer.append("contexts");
				buffer.append(":[");
			}
			buffer.append(Util.toBytesRef(context, scratch).utf8ToString());
			ContextQuery.ContextMetaData metaData = contexts.get(context);
			if ((metaData.exact) == false) {
				buffer.append("*");
			}
			if ((metaData.boost) != 0) {
				buffer.append("^");
				buffer.append(Float.toString(metaData.boost));
			}
		}
		if ((buffer.length()) != 0) {
			buffer.append("]");
			buffer.append(",");
		}
		return (buffer.toString()) + (innerQuery.toString(field));
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		final CompletionWeight innerWeight = ((CompletionWeight) (innerQuery.createWeight(searcher, needsScores, boost)));
		final Automaton innerAutomaton = innerWeight.getAutomaton();
		if ((innerAutomaton.getNumStates()) == 0) {
			return new CompletionWeight(this, innerAutomaton);
		}
		Automaton optionalSepLabel = Operations.optional(Automata.makeChar(ConcatenateGraphFilter.SEP_LABEL));
		Automaton prefixAutomaton = Operations.concatenate(optionalSepLabel, innerAutomaton);
		Automaton contextsAutomaton = Operations.concatenate(ContextQuery.toContextAutomaton(contexts, matchAllContexts), prefixAutomaton);
		contextsAutomaton = Operations.determinize(contextsAutomaton, Operations.DEFAULT_MAX_DETERMINIZED_STATES);
		final Map<IntsRef, Float> contextMap = new HashMap<>(contexts.size());
		final TreeSet<Integer> contextLengths = new TreeSet<>();
		for (Map.Entry<IntsRef, ContextQuery.ContextMetaData> entry : contexts.entrySet()) {
			ContextQuery.ContextMetaData contextMetaData = entry.getValue();
			contextMap.put(entry.getKey(), contextMetaData.boost);
			contextLengths.add(entry.getKey().length);
		}
		int[] contextLengthArray = new int[contextLengths.size()];
		final Iterator<Integer> iterator = contextLengths.descendingIterator();
		for (int i = 0; iterator.hasNext(); i++) {
			contextLengthArray[i] = iterator.next();
		}
		return new ContextQuery.ContextCompletionWeight(this, contextsAutomaton, innerWeight, contextMap, contextLengthArray);
	}

	private static Automaton toContextAutomaton(final Map<IntsRef, ContextQuery.ContextMetaData> contexts, final boolean matchAllContexts) {
		final Automaton matchAllAutomaton = Operations.repeat(Automata.makeAnyString());
		final Automaton sep = Automata.makeChar(ContextSuggestField.CONTEXT_SEPARATOR);
		if (matchAllContexts || ((contexts.size()) == 0)) {
			return Operations.concatenate(matchAllAutomaton, sep);
		}else {
			Automaton contextsAutomaton = null;
			for (Map.Entry<IntsRef, ContextQuery.ContextMetaData> entry : contexts.entrySet()) {
				final ContextQuery.ContextMetaData contextMetaData = entry.getValue();
				final IntsRef ref = entry.getKey();
				Automaton contextAutomaton = Automata.makeString(ref.ints, ref.offset, ref.length);
				if ((contextMetaData.exact) == false) {
					contextAutomaton = Operations.concatenate(contextAutomaton, matchAllAutomaton);
				}
				contextAutomaton = Operations.concatenate(contextAutomaton, sep);
				if (contextsAutomaton == null) {
					contextsAutomaton = contextAutomaton;
				}else {
					contextsAutomaton = Operations.union(contextsAutomaton, contextAutomaton);
				}
			}
			return contextsAutomaton;
		}
	}

	private static class ContextMetaData {
		private final float boost;

		private final boolean exact;

		private ContextMetaData(float boost, boolean exact) {
			this.boost = boost;
			this.exact = exact;
		}
	}

	private static class ContextCompletionWeight extends CompletionWeight {
		private final Map<IntsRef, Float> contextMap;

		private final int[] contextLengths;

		private final CompletionWeight innerWeight;

		private final BytesRefBuilder scratch = new BytesRefBuilder();

		private float currentBoost;

		private CharSequence currentContext;

		public ContextCompletionWeight(CompletionQuery query, Automaton automaton, CompletionWeight innerWeight, Map<IntsRef, Float> contextMap, int[] contextLengths) throws IOException {
			super(query, automaton);
			this.contextMap = contextMap;
			this.contextLengths = contextLengths;
			this.innerWeight = innerWeight;
		}

		@Override
		protected void setNextMatch(final IntsRef pathPrefix) {
			IntsRef ref = pathPrefix.clone();
			for (int contextLength : contextLengths) {
				if (contextLength > (pathPrefix.length)) {
					continue;
				}
				ref.length = contextLength;
				if (contextMap.containsKey(ref)) {
					currentBoost = contextMap.get(ref);
					ref.length = pathPrefix.length;
					setInnerWeight(ref, contextLength);
					return;
				}
			}
			ref.length = pathPrefix.length;
			currentBoost = 0.0F;
			setInnerWeight(ref, 0);
		}

		private void setInnerWeight(IntsRef ref, int offset) {
			IntsRefBuilder refBuilder = new IntsRefBuilder();
			for (int i = offset; i < (ref.length); i++) {
				if ((ref.ints[((ref.offset) + i)]) == (ContextSuggestField.CONTEXT_SEPARATOR)) {
					if (i > 0) {
						refBuilder.copyInts(ref.ints, ref.offset, i);
						currentContext = Util.toBytesRef(refBuilder.get(), scratch).utf8ToString();
					}else {
						currentContext = null;
					}
					ref.offset = ++i;
					assert (ref.offset) < (ref.length) : "input should not end with the context separator";
					if ((ref.ints[i]) == (ConcatenateGraphFilter.SEP_LABEL)) {
						(ref.offset)++;
						assert (ref.offset) < (ref.length) : "input should not end with a context separator followed by SEP_LABEL";
					}
					ref.length = (ref.length) - (ref.offset);
					refBuilder.copyInts(ref.ints, ref.offset, ref.length);
					return;
				}
			}
		}

		@Override
		protected CharSequence context() {
			return currentContext;
		}

		@Override
		protected float boost() {
			return 0f;
		}
	}

	@Override
	public boolean equals(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}
}

