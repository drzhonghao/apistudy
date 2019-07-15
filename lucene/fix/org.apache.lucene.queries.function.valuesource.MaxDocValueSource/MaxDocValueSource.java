

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.IndexSearcher;


public class MaxDocValueSource extends ValueSource {
	public String name() {
		return "maxdoc";
	}

	@Override
	public String description() {
		return (name()) + "()";
	}

	@Override
	public void createWeight(Map context, IndexSearcher searcher) throws IOException {
		context.put("searcher", searcher);
	}

	@Override
	public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
		IndexSearcher searcher = ((IndexSearcher) (context.get("searcher")));
		return null;
	}

	@Override
	public boolean equals(Object o) {
		return (this.getClass()) == (o.getClass());
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode();
	}
}

