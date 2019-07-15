

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;


public class PhraseQuery extends Query {
	public static class Builder {
		private int slop;

		private final List<Term> terms;

		private final List<Integer> positions;

		public Builder() {
			slop = 0;
			terms = new ArrayList<>();
			positions = new ArrayList<>();
		}

		public PhraseQuery.Builder setSlop(int slop) {
			this.slop = slop;
			return this;
		}

		public PhraseQuery.Builder add(Term term) {
			return add(term, (positions.isEmpty() ? 0 : 1 + (positions.get(((positions.size()) - 1)))));
		}

		public PhraseQuery.Builder add(Term term, int position) {
			if (position < 0) {
				throw new IllegalArgumentException(("Positions must be >= 0, got " + position));
			}
			if ((positions.isEmpty()) == false) {
				final int lastPosition = positions.get(((positions.size()) - 1));
				if (position < lastPosition) {
					throw new IllegalArgumentException(((("Positions must be added in order, got " + position) + " after ") + lastPosition));
				}
			}
			if (((terms.isEmpty()) == false) && ((term.field().equals(terms.get(0).field())) == false)) {
				throw new IllegalArgumentException(((("All terms must be on the same field, got " + (term.field())) + " and ") + (terms.get(0).field())));
			}
			terms.add(term);
			positions.add(position);
			return this;
		}

		public PhraseQuery build() {
			Term[] terms = this.terms.toArray(new Term[this.terms.size()]);
			int[] positions = new int[this.positions.size()];
			for (int i = 0; i < (positions.length); ++i) {
				positions[i] = this.positions.get(i);
			}
			return new PhraseQuery(slop, terms, positions);
		}
	}

	private final int slop;

	private final String field;

	private final Term[] terms;

	private final int[] positions;

	private PhraseQuery(int slop, Term[] terms, int[] positions) {
		if ((terms.length) != (positions.length)) {
			throw new IllegalArgumentException("Must have as many terms as positions");
		}
		if (slop < 0) {
			throw new IllegalArgumentException(("Slop must be >= 0, got " + slop));
		}
		for (int i = 1; i < (terms.length); ++i) {
			if ((terms[(i - 1)].field().equals(terms[i].field())) == false) {
				throw new IllegalArgumentException("All terms should have the same field");
			}
		}
		for (int position : positions) {
			if (position < 0) {
				throw new IllegalArgumentException(("Positions must be >= 0, got " + position));
			}
		}
		for (int i = 1; i < (positions.length); ++i) {
			if ((positions[i]) < (positions[(i - 1)])) {
				throw new IllegalArgumentException(((("Positions should not go backwards, got " + (positions[(i - 1)])) + " before ") + (positions[i])));
			}
		}
		this.slop = slop;
		this.terms = terms;
		this.positions = positions;
		this.field = ((terms.length) == 0) ? null : terms[0].field();
	}

	private static int[] incrementalPositions(int length) {
		int[] positions = new int[length];
		for (int i = 0; i < length; ++i) {
			positions[i] = i;
		}
		return positions;
	}

	private static Term[] toTerms(String field, String... termStrings) {
		Term[] terms = new Term[termStrings.length];
		for (int i = 0; i < (terms.length); ++i) {
			terms[i] = new Term(field, termStrings[i]);
		}
		return terms;
	}

	private static Term[] toTerms(String field, BytesRef... termBytes) {
		Term[] terms = new Term[termBytes.length];
		for (int i = 0; i < (terms.length); ++i) {
			terms[i] = new Term(field, termBytes[i]);
		}
		return terms;
	}

	public PhraseQuery(int slop, String field, String... terms) {
		this(slop, PhraseQuery.toTerms(field, terms), PhraseQuery.incrementalPositions(terms.length));
	}

	public PhraseQuery(String field, String... terms) {
		this(0, field, terms);
	}

	public PhraseQuery(int slop, String field, BytesRef... terms) {
		this(slop, PhraseQuery.toTerms(field, terms), PhraseQuery.incrementalPositions(terms.length));
	}

	public PhraseQuery(String field, BytesRef... terms) {
		this(0, field, terms);
	}

	public int getSlop() {
		return slop;
	}

	public Term[] getTerms() {
		return terms;
	}

	public int[] getPositions() {
		return positions;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if ((terms.length) == 0) {
			return new MatchNoDocsQuery("empty PhraseQuery");
		}else
			if ((terms.length) == 1) {
				return new TermQuery(terms[0]);
			}else
				if ((positions[0]) != 0) {
					int[] newPositions = new int[positions.length];
					for (int i = 0; i < (positions.length); ++i) {
						newPositions[i] = (positions[i]) - (positions[0]);
					}
					return new PhraseQuery(slop, terms, newPositions);
				}else {
					return super.rewrite(reader);
				}


	}

	static class PostingsAndFreq implements Comparable<PhraseQuery.PostingsAndFreq> {
		final PostingsEnum postings;

		final int position;

		final Term[] terms;

		final int nTerms;

		public PostingsAndFreq(PostingsEnum postings, int position, Term... terms) {
			this.postings = postings;
			this.position = position;
			nTerms = (terms == null) ? 0 : terms.length;
			if ((nTerms) > 0) {
				if ((terms.length) == 1) {
					this.terms = terms;
				}else {
					Term[] terms2 = new Term[terms.length];
					System.arraycopy(terms, 0, terms2, 0, terms.length);
					Arrays.sort(terms2);
					this.terms = terms2;
				}
			}else {
				this.terms = null;
			}
		}

		@Override
		public int compareTo(PhraseQuery.PostingsAndFreq other) {
			if ((position) != (other.position)) {
				return (position) - (other.position);
			}
			if ((nTerms) != (other.nTerms)) {
				return (nTerms) - (other.nTerms);
			}
			if ((nTerms) == 0) {
				return 0;
			}
			for (int i = 0; i < (terms.length); i++) {
				int res = terms[i].compareTo(other.terms[i]);
				if (res != 0)
					return res;

			}
			return 0;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + (position);
			for (int i = 0; i < (nTerms); i++) {
				result = (prime * result) + (terms[i].hashCode());
			}
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ((this) == obj)
				return true;

			if (obj == null)
				return false;

			if ((getClass()) != (obj.getClass()))
				return false;

			PhraseQuery.PostingsAndFreq other = ((PhraseQuery.PostingsAndFreq) (obj));
			if ((position) != (other.position))
				return false;

			if ((terms) == null)
				return (other.terms) == null;

			return Arrays.equals(terms, other.terms);
		}
	}

	private static final int TERM_POSNS_SEEK_OPS_PER_DOC = 128;

	private static final int TERM_OPS_PER_POS = 7;

	static float termPositionsCost(TermsEnum termsEnum) throws IOException {
		int docFreq = termsEnum.docFreq();
		assert docFreq > 0;
		long totalTermFreq = termsEnum.totalTermFreq();
		float expOccurrencesInMatchingDoc = (totalTermFreq < docFreq) ? 1 : totalTermFreq / ((float) (docFreq));
		return (PhraseQuery.TERM_POSNS_SEEK_OPS_PER_DOC) + (expOccurrencesInMatchingDoc * (PhraseQuery.TERM_OPS_PER_POS));
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		return null;
	}

	private static boolean termNotInReader(LeafReader reader, Term term) throws IOException {
		return (reader.docFreq(term)) == 0;
	}

	@Override
	public String toString(String f) {
		StringBuilder buffer = new StringBuilder();
		if (((field) != null) && (!(field.equals(f)))) {
			buffer.append(field);
			buffer.append(":");
		}
		buffer.append("\"");
		final int maxPosition;
		if ((positions.length) == 0) {
			maxPosition = -1;
		}else {
			maxPosition = positions[((positions.length) - 1)];
		}
		String[] pieces = new String[maxPosition + 1];
		for (int i = 0; i < (terms.length); i++) {
			int pos = positions[i];
			String s = pieces[pos];
			if (s == null) {
				s = terms[i].text();
			}else {
				s = (s + "|") + (terms[i].text());
			}
			pieces[pos] = s;
		}
		for (int i = 0; i < (pieces.length); i++) {
			if (i > 0) {
				buffer.append(' ');
			}
			String s = pieces[i];
			if (s == null) {
				buffer.append('?');
			}else {
				buffer.append(s);
			}
		}
		buffer.append("\"");
		if ((slop) != 0) {
			buffer.append("~");
			buffer.append(slop);
		}
		return buffer.toString();
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(PhraseQuery other) {
		return (((slop) == (other.slop)) && (Arrays.equals(terms, other.terms))) && (Arrays.equals(positions, other.positions));
	}

	@Override
	public int hashCode() {
		int h = classHash();
		h = (31 * h) + (slop);
		h = (31 * h) + (Arrays.hashCode(terms));
		h = (31 * h) + (Arrays.hashCode(positions));
		return h;
	}
}

