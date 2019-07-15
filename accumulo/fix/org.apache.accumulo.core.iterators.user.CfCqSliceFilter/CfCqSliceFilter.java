

import java.io.IOException;
import java.util.Map;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.OptionDescriber;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.user.CfCqSliceOpts;


public class CfCqSliceFilter extends Filter {
	private CfCqSliceOpts cso;

	@Override
	public void init(SortedKeyValueIterator<Key, Value> source, Map<String, String> options, IteratorEnvironment env) throws IOException {
		super.init(source, options, env);
		cso = new CfCqSliceOpts(options);
	}

	@Override
	public boolean accept(Key k, Value v) {
		PartialKey inSlice = isKeyInSlice(k);
		return inSlice == (PartialKey.ROW_COLFAM_COLQUAL);
	}

	@Override
	public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
		CfCqSliceFilter o = ((CfCqSliceFilter) (super.deepCopy(env)));
		o.cso = new CfCqSliceOpts(cso);
		return o;
	}

	private PartialKey isKeyInSlice(Key k) {
		return PartialKey.ROW_COLFAM_COLQUAL;
	}

	@Override
	public OptionDescriber.IteratorOptions describeOptions() {
		return null;
	}

	@Override
	public boolean validateOptions(Map<String, String> options) {
		return false;
	}
}

