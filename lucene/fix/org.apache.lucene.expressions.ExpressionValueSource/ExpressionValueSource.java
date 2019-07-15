

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.lucene.expressions.Bindings;
import org.apache.lucene.expressions.Expression;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SegmentCacheable;


@SuppressWarnings({ "rawtypes", "unchecked" })
final class ExpressionValueSource extends DoubleValuesSource {
	final DoubleValuesSource[] variables;

	final Expression expression;

	final boolean needsScores;

	ExpressionValueSource(Bindings bindings, Expression expression) {
		if (bindings == null)
			throw new NullPointerException();

		this.expression = Objects.requireNonNull(expression);
		variables = new DoubleValuesSource[expression.variables.length];
		boolean needsScores = false;
		for (int i = 0; i < (variables.length); i++) {
			DoubleValuesSource source = bindings.getDoubleValuesSource(expression.variables[i]);
			if (source == null) {
				throw new RuntimeException((("Internal error. Variable (" + (expression.variables[i])) + ") does not exist."));
			}
			needsScores |= source.needsScores();
			variables[i] = source;
		}
		this.needsScores = needsScores;
	}

	ExpressionValueSource(DoubleValuesSource[] variables, Expression expression, boolean needsScores) {
		this.variables = variables;
		this.expression = expression;
		this.needsScores = needsScores;
	}

	@Override
	public DoubleValues getValues(LeafReaderContext readerContext, DoubleValues scores) throws IOException {
		Map<String, DoubleValues> valuesCache = new HashMap<>();
		DoubleValues[] externalValues = new DoubleValues[expression.variables.length];
		for (int i = 0; i < (variables.length); ++i) {
			String externalName = expression.variables[i];
			DoubleValues values = valuesCache.get(externalName);
			if (values == null) {
				values = variables[i].getValues(readerContext, scores);
				if (values == null) {
					throw new RuntimeException((("Internal error. External (" + externalName) + ") does not exist."));
				}
				valuesCache.put(externalName, values);
			}
			externalValues[i] = ExpressionValueSource.zeroWhenUnpositioned(values);
		}
		return null;
	}

	private static DoubleValues zeroWhenUnpositioned(DoubleValues in) {
		return new DoubleValues() {
			boolean positioned = false;

			@Override
			public double doubleValue() throws IOException {
				return positioned ? in.doubleValue() : 0;
			}

			@Override
			public boolean advanceExact(int doc) throws IOException {
				return positioned = in.advanceExact(doc);
			}
		};
	}

	@Override
	public String toString() {
		return ("expr(" + (expression.sourceText)) + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((expression) == null ? 0 : expression.sourceText.hashCode());
		result = (prime * result) + (needsScores ? 1231 : 1237);
		result = (prime * result) + (Arrays.hashCode(variables));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ((this) == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if ((getClass()) != (obj.getClass())) {
			return false;
		}
		ExpressionValueSource other = ((ExpressionValueSource) (obj));
		if ((expression) == null) {
			if ((other.expression) != null) {
				return false;
			}
		}else
			if (!(expression.sourceText.equals(other.expression.sourceText))) {
				return false;
			}

		if ((needsScores) != (other.needsScores)) {
			return false;
		}
		if (!(Arrays.equals(variables, other.variables))) {
			return false;
		}
		return true;
	}

	@Override
	public boolean needsScores() {
		return needsScores;
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx) {
		for (DoubleValuesSource v : variables) {
			if ((v.isCacheable(ctx)) == false)
				return false;

		}
		return true;
	}

	@Override
	public Explanation explain(LeafReaderContext ctx, int docId, Explanation scoreExplanation) throws IOException {
		Explanation[] explanations = new Explanation[variables.length];
		DoubleValues dv = getValues(ctx, DoubleValuesSource.constant(scoreExplanation.getValue()).getValues(ctx, null));
		if ((dv.advanceExact(docId)) == false) {
			return Explanation.noMatch(expression.sourceText);
		}
		int i = 0;
		for (DoubleValuesSource var : variables) {
			explanations[(i++)] = var.explain(ctx, docId, scoreExplanation);
		}
		return Explanation.match(((float) (dv.doubleValue())), ((expression.sourceText) + ", computed from:"), explanations);
	}

	@Override
	public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
		boolean changed = false;
		DoubleValuesSource[] rewritten = new DoubleValuesSource[variables.length];
		for (int i = 0; i < (variables.length); i++) {
			rewritten[i] = variables[i].rewrite(searcher);
			changed |= (rewritten[i]) == (variables[i]);
		}
		if (changed) {
			return new ExpressionValueSource(rewritten, expression, needsScores);
		}
		return this;
	}
}

