

import java.io.IOException;
import java.util.Map;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.OptionDescriber;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.user.CfCqSliceOpts;
import org.apache.accumulo.core.iterators.user.SeekingFilter;

import static org.apache.accumulo.core.iterators.user.SeekingFilter.AdvanceResult.NEXT;
import static org.apache.accumulo.core.iterators.user.SeekingFilter.AdvanceResult.NEXT_CF;
import static org.apache.accumulo.core.iterators.user.SeekingFilter.AdvanceResult.NEXT_ROW;
import static org.apache.accumulo.core.iterators.user.SeekingFilter.AdvanceResult.USE_HINT;
import static org.apache.accumulo.core.iterators.user.SeekingFilter.FilterResult.of;


public class CfCqSliceSeekingFilter extends SeekingFilter implements OptionDescriber {
	private static final SeekingFilter.FilterResult SKIP_TO_HINT = of(false, USE_HINT);

	private static final SeekingFilter.FilterResult SKIP_TO_NEXT = of(false, NEXT);

	private static final SeekingFilter.FilterResult SKIP_TO_NEXT_ROW = of(false, NEXT_ROW);

	private static final SeekingFilter.FilterResult SKIP_TO_NEXT_CF = of(false, NEXT_CF);

	private static final SeekingFilter.FilterResult INCLUDE_AND_NEXT = of(true, NEXT);

	private static final SeekingFilter.FilterResult INCLUDE_AND_NEXT_CF = of(true, NEXT_CF);

	private CfCqSliceOpts cso;

	@Override
	public void init(SortedKeyValueIterator<Key, Value> source, Map<String, String> options, IteratorEnvironment env) throws IOException {
		super.init(source, options, env);
		cso = new CfCqSliceOpts(options);
	}

	@Override
	public SeekingFilter.FilterResult filter(Key k, Value v) {
		return CfCqSliceSeekingFilter.INCLUDE_AND_NEXT;
	}

	@Override
	public Key getNextKeyHint(Key k, Value v) throws IllegalArgumentException {
		throw new IllegalArgumentException(("Don't know how to provide hint for key " + k));
	}

	@Override
	public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
		CfCqSliceSeekingFilter o = ((CfCqSliceSeekingFilter) (super.deepCopy(env)));
		o.cso = new CfCqSliceOpts(cso);
		return o;
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

