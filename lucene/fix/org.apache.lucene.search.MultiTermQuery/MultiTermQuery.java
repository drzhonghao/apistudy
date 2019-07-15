

import java.io.IOException;
import java.util.Objects;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BlendedTermQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopTermsRewrite;
import org.apache.lucene.util.AttributeSource;

import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


public abstract class MultiTermQuery extends Query {
	protected final String field;

	protected MultiTermQuery.RewriteMethod rewriteMethod = MultiTermQuery.CONSTANT_SCORE_REWRITE;

	public static abstract class RewriteMethod {
		public abstract Query rewrite(IndexReader reader, MultiTermQuery query) throws IOException;

		protected TermsEnum getTermsEnum(MultiTermQuery query, Terms terms, AttributeSource atts) throws IOException {
			return query.getTermsEnum(terms, atts);
		}
	}

	public static final MultiTermQuery.RewriteMethod CONSTANT_SCORE_REWRITE = new MultiTermQuery.RewriteMethod() {
		@Override
		public Query rewrite(IndexReader reader, MultiTermQuery query) {
			return null;
		}
	};

	public static final class TopTermsScoringBooleanQueryRewrite extends TopTermsRewrite<BooleanQuery.Builder> {
		public TopTermsScoringBooleanQueryRewrite(int size) {
			super(size);
		}

		@Override
		protected int getMaxSize() {
			return BooleanQuery.getMaxClauseCount();
		}

		@Override
		protected BooleanQuery.Builder getTopLevelBuilder() {
			return new BooleanQuery.Builder();
		}

		@Override
		protected Query build(BooleanQuery.Builder builder) {
			return builder.build();
		}

		@Override
		protected void addClause(BooleanQuery.Builder topLevel, Term term, int docCount, float boost, TermContext states) {
			final TermQuery tq = new TermQuery(term, states);
			topLevel.add(new BoostQuery(tq, boost), SHOULD);
		}
	}

	public static final class TopTermsBlendedFreqScoringRewrite extends TopTermsRewrite<BlendedTermQuery.Builder> {
		public TopTermsBlendedFreqScoringRewrite(int size) {
			super(size);
		}

		@Override
		protected int getMaxSize() {
			return BooleanQuery.getMaxClauseCount();
		}

		@Override
		protected BlendedTermQuery.Builder getTopLevelBuilder() {
			BlendedTermQuery.Builder builder = new BlendedTermQuery.Builder();
			builder.setRewriteMethod(BlendedTermQuery.BOOLEAN_REWRITE);
			return builder;
		}

		@Override
		protected Query build(BlendedTermQuery.Builder builder) {
			return builder.build();
		}

		@Override
		protected void addClause(BlendedTermQuery.Builder topLevel, Term term, int docCount, float boost, TermContext states) {
			topLevel.add(term, boost, states);
		}
	}

	public static final class TopTermsBoostOnlyBooleanQueryRewrite extends TopTermsRewrite<BooleanQuery.Builder> {
		public TopTermsBoostOnlyBooleanQueryRewrite(int size) {
			super(size);
		}

		@Override
		protected int getMaxSize() {
			return BooleanQuery.getMaxClauseCount();
		}

		@Override
		protected BooleanQuery.Builder getTopLevelBuilder() {
			return new BooleanQuery.Builder();
		}

		@Override
		protected Query build(BooleanQuery.Builder builder) {
			return builder.build();
		}

		@Override
		protected void addClause(BooleanQuery.Builder topLevel, Term term, int docFreq, float boost, TermContext states) {
			final Query q = new ConstantScoreQuery(new TermQuery(term, states));
			topLevel.add(new BoostQuery(q, boost), SHOULD);
		}
	}

	public MultiTermQuery(final String field) {
		this.field = Objects.requireNonNull(field, "field must not be null");
	}

	public final String getField() {
		return field;
	}

	protected abstract TermsEnum getTermsEnum(Terms terms, AttributeSource atts) throws IOException;

	protected final TermsEnum getTermsEnum(Terms terms) throws IOException {
		return getTermsEnum(terms, new AttributeSource());
	}

	@Override
	public final Query rewrite(IndexReader reader) throws IOException {
		return rewriteMethod.rewrite(reader, this);
	}

	public MultiTermQuery.RewriteMethod getRewriteMethod() {
		return rewriteMethod;
	}

	public void setRewriteMethod(MultiTermQuery.RewriteMethod method) {
		rewriteMethod = method;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = classHash();
		result = (prime * result) + (rewriteMethod.hashCode());
		result = (prime * result) + (field.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(MultiTermQuery other) {
		return (rewriteMethod.equals(other.rewriteMethod)) && (field.equals(other.field));
	}
}

