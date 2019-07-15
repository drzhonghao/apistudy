

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.MultiFunction;


public class DefFunction extends MultiFunction {
	public DefFunction(List<ValueSource> sources) {
		super(sources);
	}

	@Override
	protected String name() {
		return "def";
	}

	@Override
	public FunctionValues getValues(Map fcontext, LeafReaderContext readerContext) throws IOException {
		return null;
	}
}

