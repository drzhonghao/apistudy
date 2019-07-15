

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.index.sasi.Term;
import org.apache.cassandra.index.sasi.disk.OnDiskIndex;
import org.apache.cassandra.index.sasi.disk.OnDiskIndex.DataTerm;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.disk.TokenTreeBuilder;
import org.apache.cassandra.index.sasi.utils.CombinedValue;
import org.apache.cassandra.index.sasi.utils.RangeIterator;
import org.apache.cassandra.index.sasi.utils.RangeUnionIterator;


public class CombinedTerm implements CombinedValue<OnDiskIndex.DataTerm> {
	private final AbstractType<?> comparator;

	private final OnDiskIndex.DataTerm term;

	private final List<OnDiskIndex.DataTerm> mergedTerms = new ArrayList<>();

	public CombinedTerm(AbstractType<?> comparator, OnDiskIndex.DataTerm term) {
		this.comparator = comparator;
		this.term = term;
	}

	public ByteBuffer getTerm() {
		return term.getTerm();
	}

	public boolean isPartial() {
		return term.isPartial();
	}

	public RangeIterator<Long, Token> getTokenIterator() {
		RangeIterator.Builder<Long, Token> union = RangeUnionIterator.builder();
		union.add(term.getTokens());
		mergedTerms.stream().map(OnDiskIndex.DataTerm::getTokens).forEach(union::add);
		return union.build();
	}

	public TokenTreeBuilder getTokenTreeBuilder() {
		return null;
	}

	public void merge(CombinedValue<OnDiskIndex.DataTerm> other) {
		if (!(other instanceof CombinedTerm))
			return;

		CombinedTerm o = ((CombinedTerm) (other));
		assert (comparator) == (o.comparator);
		mergedTerms.add(o.term);
	}

	public OnDiskIndex.DataTerm get() {
		return term;
	}

	public int compareTo(CombinedValue<OnDiskIndex.DataTerm> o) {
		return term.compareTo(comparator, o.get().getTerm());
	}
}

