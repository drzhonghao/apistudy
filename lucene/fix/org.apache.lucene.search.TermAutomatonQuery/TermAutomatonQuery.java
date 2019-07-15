

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.Transition;


public class TermAutomatonQuery extends Query {
	private final String field;

	private final Automaton.Builder builder;

	Automaton det;

	private final Map<BytesRef, Integer> termToID = new HashMap<>();

	private final Map<Integer, BytesRef> idToTerm = new HashMap<>();

	private int anyTermID = -1;

	public TermAutomatonQuery(String field) {
		this.field = field;
		this.builder = new Automaton.Builder();
	}

	public int createState() {
		return builder.createState();
	}

	public void setAccept(int state, boolean accept) {
		builder.setAccept(state, accept);
	}

	public void addTransition(int source, int dest, String term) {
		addTransition(source, dest, new BytesRef(term));
	}

	public void addTransition(int source, int dest, BytesRef term) {
		if (term == null) {
			throw new NullPointerException("term should not be null");
		}
		builder.addTransition(source, dest, getTermID(term));
	}

	public void addAnyTransition(int source, int dest) {
		builder.addTransition(source, dest, getTermID(null));
	}

	public void finish() {
		finish(Operations.DEFAULT_MAX_DETERMINIZED_STATES);
	}

	public void finish(int maxDeterminizedStates) {
		Automaton automaton = builder.finish();
		Transition t = new Transition();
		if ((anyTermID) != (-1)) {
			int count = automaton.initTransition(0, t);
			for (int i = 0; i < count; i++) {
				automaton.getNextTransition(t);
				if (((anyTermID) >= (t.min)) && ((anyTermID) <= (t.max))) {
					throw new IllegalStateException("automaton cannot lead with an ANY transition");
				}
			}
			int numStates = automaton.getNumStates();
			for (int i = 0; i < numStates; i++) {
				count = automaton.initTransition(i, t);
				for (int j = 0; j < count; j++) {
					automaton.getNextTransition(t);
					if (((automaton.isAccept(t.dest)) && ((anyTermID) >= (t.min))) && ((anyTermID) <= (t.max))) {
						throw new IllegalStateException("automaton cannot end with an ANY transition");
					}
				}
			}
			int termCount = termToID.size();
			Automaton newAutomaton = new Automaton();
			for (int i = 0; i < numStates; i++) {
				newAutomaton.createState();
				newAutomaton.setAccept(i, automaton.isAccept(i));
			}
			for (int i = 0; i < numStates; i++) {
				count = automaton.initTransition(i, t);
				for (int j = 0; j < count; j++) {
					automaton.getNextTransition(t);
					int min;
					int max;
					if (((t.min) <= (anyTermID)) && ((anyTermID) <= (t.max))) {
						min = 0;
						max = termCount - 1;
					}else {
						min = t.min;
						max = t.max;
					}
					newAutomaton.addTransition(t.source, t.dest, min, max);
				}
			}
			newAutomaton.finishState();
			automaton = newAutomaton;
		}
		det = Operations.removeDeadStates(Operations.determinize(automaton, maxDeterminizedStates));
		if (det.isAccept(0)) {
			throw new IllegalStateException("cannot accept the empty string");
		}
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		IndexReaderContext context = searcher.getTopReaderContext();
		Map<Integer, TermContext> termStates = new HashMap<>();
		for (Map.Entry<BytesRef, Integer> ent : termToID.entrySet()) {
			if ((ent.getKey()) != null) {
				termStates.put(ent.getValue(), TermContext.build(context, new Term(field, ent.getKey())));
			}
		}
		return new TermAutomatonQuery.TermAutomatonWeight(det, searcher, termStates, boost);
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append("TermAutomatonQuery(field=");
		sb.append(this.field);
		if ((det) != null) {
			sb.append(" numStates=");
			sb.append(det.getNumStates());
		}
		sb.append(')');
		return sb.toString();
	}

	private int getTermID(BytesRef term) {
		Integer id = termToID.get(term);
		if (id == null) {
			id = termToID.size();
			if (term != null) {
				term = BytesRef.deepCopyOf(term);
			}
			termToID.put(term, id);
			idToTerm.put(id, term);
			if (term == null) {
				anyTermID = id;
			}
		}
		return id;
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private static boolean checkFinished(TermAutomatonQuery q) {
		if ((q.det) == null) {
			throw new IllegalStateException(("Call finish first on: " + q));
		}
		return true;
	}

	private boolean equalsTo(TermAutomatonQuery other) {
		return ((TermAutomatonQuery.checkFinished(this)) && (TermAutomatonQuery.checkFinished(other))) && (other == (this));
	}

	@Override
	public int hashCode() {
		TermAutomatonQuery.checkFinished(this);
		return System.identityHashCode(this);
	}

	public String toDot() {
		StringBuilder b = new StringBuilder();
		b.append("digraph Automaton {\n");
		b.append("  rankdir = LR\n");
		final int numStates = det.getNumStates();
		if (numStates > 0) {
			b.append("  initial [shape=plaintext,label=\"0\"]\n");
			b.append("  initial -> 0\n");
		}
		Transition t = new Transition();
		for (int state = 0; state < numStates; state++) {
			b.append("  ");
			b.append(state);
			if (det.isAccept(state)) {
				b.append(((" [shape=doublecircle,label=\"" + state) + "\"]\n"));
			}else {
				b.append(((" [shape=circle,label=\"" + state) + "\"]\n"));
			}
			int numTransitions = det.initTransition(state, t);
			for (int i = 0; i < numTransitions; i++) {
				det.getNextTransition(t);
				assert (t.max) >= (t.min);
				for (int j = t.min; j <= (t.max); j++) {
					b.append("  ");
					b.append(state);
					b.append(" -> ");
					b.append(t.dest);
					b.append(" [label=\"");
					if (j == (anyTermID)) {
						b.append('*');
					}else {
						b.append(idToTerm.get(j).utf8ToString());
					}
					b.append("\"]\n");
				}
			}
		}
		b.append('}');
		return b.toString();
	}

	static class EnumAndScorer {
		public final int termID;

		public final PostingsEnum posEnum;

		public int posLeft;

		public int pos;

		public EnumAndScorer(int termID, PostingsEnum posEnum) {
			this.termID = termID;
			this.posEnum = posEnum;
		}
	}

	final class TermAutomatonWeight extends Weight {
		final Automaton automaton;

		private final Map<Integer, TermContext> termStates;

		private final Similarity.SimWeight stats;

		private final Similarity similarity;

		public TermAutomatonWeight(Automaton automaton, IndexSearcher searcher, Map<Integer, TermContext> termStates, float boost) throws IOException {
			super(TermAutomatonQuery.this);
			this.automaton = automaton;
			this.termStates = termStates;
			this.similarity = searcher.getSimilarity(true);
			List<TermStatistics> allTermStats = new ArrayList<>();
			for (Map.Entry<Integer, BytesRef> ent : idToTerm.entrySet()) {
				Integer termID = ent.getKey();
				if ((ent.getValue()) != null) {
					allTermStats.add(searcher.termStatistics(new Term(field, ent.getValue()), termStates.get(termID)));
				}
			}
			stats = similarity.computeWeight(boost, searcher.collectionStatistics(field), allTermStats.toArray(new TermStatistics[allTermStats.size()]));
		}

		@Override
		public void extractTerms(Set<Term> terms) {
			for (BytesRef text : termToID.keySet()) {
				if (text != null) {
					terms.add(new Term(field, text));
				}
			}
		}

		@Override
		public String toString() {
			return ("weight(" + (TermAutomatonQuery.this)) + ")";
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			TermAutomatonQuery.EnumAndScorer[] enums = new TermAutomatonQuery.EnumAndScorer[idToTerm.size()];
			boolean any = false;
			for (Map.Entry<Integer, TermContext> ent : termStates.entrySet()) {
				TermContext termContext = ent.getValue();
				assert termContext.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader (" + (ReaderUtil.getTopLevelContext(context));
				BytesRef term = idToTerm.get(ent.getKey());
				TermState state = termContext.get(context.ord);
				if (state != null) {
					TermsEnum termsEnum = context.reader().terms(field).iterator();
					termsEnum.seekExact(term, state);
					enums[ent.getKey()] = new TermAutomatonQuery.EnumAndScorer(ent.getKey(), termsEnum.postings(null, PostingsEnum.POSITIONS));
					any = true;
				}
			}
			if (any) {
			}else {
				return null;
			}
			return null;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return true;
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			return null;
		}
	}

	public Query rewrite(IndexReader reader) throws IOException {
		if (Operations.isEmpty(det)) {
			return new MatchNoDocsQuery();
		}
		IntsRef single = Operations.getSingleton(det);
		if ((single != null) && ((single.length) == 1)) {
			return new TermQuery(new Term(field, idToTerm.get(single.ints[single.offset])));
		}
		MultiPhraseQuery.Builder mpq = new MultiPhraseQuery.Builder();
		PhraseQuery.Builder pq = new PhraseQuery.Builder();
		Transition t = new Transition();
		int state = 0;
		int pos = 0;
		query : while (true) {
			int count = det.initTransition(state, t);
			if (count == 0) {
				if ((det.isAccept(state)) == false) {
					mpq = null;
					pq = null;
				}
				break;
			}else
				if (det.isAccept(state)) {
					mpq = null;
					pq = null;
					break;
				}

			int dest = -1;
			List<Term> terms = new ArrayList<>();
			boolean matchesAny = false;
			for (int i = 0; i < count; i++) {
				det.getNextTransition(t);
				if (i == 0) {
					dest = t.dest;
				}else
					if (dest != (t.dest)) {
						mpq = null;
						pq = null;
						break query;
					}

				matchesAny |= ((anyTermID) >= (t.min)) && ((anyTermID) <= (t.max));
				if (matchesAny == false) {
					for (int termID = t.min; termID <= (t.max); termID++) {
						terms.add(new Term(field, idToTerm.get(termID)));
					}
				}
			}
			if (matchesAny == false) {
				mpq.add(terms.toArray(new Term[terms.size()]), pos);
				if (pq != null) {
					if ((terms.size()) == 1) {
						pq.add(terms.get(0), pos);
					}else {
						pq = null;
					}
				}
			}
			state = dest;
			pos++;
		} 
		if (pq != null) {
			return pq.build();
		}else
			if (mpq != null) {
				return mpq.build();
			}

		return this;
	}
}

