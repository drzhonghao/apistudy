

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;


public class DocValuesNumbersQuery extends Query {
	private final String field;

	public DocValuesNumbersQuery(String field, long[] numbers) {
		this.field = Objects.requireNonNull(field);
	}

	public DocValuesNumbersQuery(String field, Collection<Long> numbers) {
		this.field = Objects.requireNonNull(field);
	}

	public DocValuesNumbersQuery(String field, Long... numbers) {
		this(field, new HashSet<Long>(Arrays.asList(numbers)));
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(DocValuesNumbersQuery other) {
		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	public String getField() {
		return field;
	}

	public Set<Long> getNumbers() {
		return null;
	}

	@Override
	public String toString(String defaultField) {
		return null;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		return new ConstantScoreWeight(this, boost) {
			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				final SortedNumericDocValues values = DocValues.getSortedNumeric(context.reader(), field);
				return new ConstantScoreScorer(this, score(), new TwoPhaseIterator(values) {
					@Override
					public boolean matches() throws IOException {
						int count = values.docValueCount();
						for (int i = 0; i < count; i++) {
						}
						return false;
					}

					@Override
					public float matchCost() {
						return 5;
					}
				});
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				return true;
			}
		};
	}
}

