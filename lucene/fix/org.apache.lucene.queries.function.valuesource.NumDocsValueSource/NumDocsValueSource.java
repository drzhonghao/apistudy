

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;


public class NumDocsValueSource extends ValueSource {
	public String name() {
		return "numdocs";
	}

	@Override
	public String description() {
		return (name()) + "()";
	}

	@Override
	public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
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

