

import org.apache.lucene.expressions.Bindings;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Rescorer;
import org.apache.lucene.search.SortField;


public abstract class Expression {
	public final String sourceText;

	public final String[] variables;

	protected Expression(String sourceText, String[] variables) {
		this.sourceText = sourceText;
		this.variables = variables;
	}

	public abstract double evaluate(DoubleValues[] functionValues);

	public DoubleValuesSource getDoubleValuesSource(Bindings bindings) {
		return null;
	}

	public SortField getSortField(Bindings bindings, boolean reverse) {
		return getDoubleValuesSource(bindings).getSortField(reverse);
	}

	public Rescorer getRescorer(Bindings bindings) {
		return null;
	}
}

